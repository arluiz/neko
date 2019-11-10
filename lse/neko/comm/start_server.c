#include <stdio.h>
#include <errno.h>
#include <time.h>
#include <unistd.h>
#include <string.h>
#include <sys/wait.h>
#include <stdlib.h>

#define LINEBUFSIZE 1000
#define DATEBUFSIZE 30
#define DATEFORMAT "%Y%m%d-%H%M%S: "
#define OUTPUT_MESSAGE "Ready\n"

#define MAX_MOVE 200
char* output_file = "/dev/null";
int timestamp = 0;
char* move_files[MAX_MOVE];
int num_move_files = 0;
int unbuffered = 0;

int start_server(char* args[]) {
    
  int p[2];
  int child_pid;
  
  pipe(p);
  
  child_pid = fork();
  if (child_pid == 0) {

    /* stdout goes to the pipe */
    close(p[0]); 
    dup2(p[1], STDOUT_FILENO);
    dup2(p[1], STDERR_FILENO);
    close(p[1]); 
    
    execvp(args[0], args);
    /* error in exec */
    perror("Error executing the server:"); 
    exit(2);

  } else if (child_pid <0) {
      perror("fork:");
      exit(5);
  }

  close(p[1]);
  dup2(p[0], STDIN_FILENO);
  close(p[0]);

  return child_pid;

}


void start_of_line(FILE* f)
{
  static char datebuf[DATEBUFSIZE];
  struct tm* timeptr;
  time_t t;

  t = time(NULL);
  timeptr = localtime(&t);
  strftime(datebuf, DATEBUFSIZE, DATEFORMAT, timeptr);
  fputs(datebuf, f);
}

static char linebuf[LINEBUFSIZE];

void log_output(const char* logfile) {

  FILE* f;
  int line_starting = 1;
  
  f = fopen(logfile, "w");
  if (f == NULL) {
    perror("Opening log file:");
    exit(4);
  }
  
  do {
    
    if (timestamp && line_starting) {
      start_of_line(f);
    }
    if (EOF == fputs(linebuf, f)) {
      perror("Writing:");
      exit(2);
    }
    if (unbuffered) {
	if (EOF == fflush(f)) {
	    perror("Writing:");
	    exit(3);
	}
    }
    line_starting =
      linebuf[0] != '\0'
      && linebuf[strlen(linebuf)-1] == '\n';
    
  } while (fgets(linebuf, LINEBUFSIZE, stdin));
  
  fclose(f);

  if (errno) {
    perror("Reading:");
    exit(3);
  }

}

void usage()
{
  fprintf(stderr,
	  "Usage: start_server [switches] command arguments\n"
	  "\n"
	  "Switches:\n"
	  "  -o output_file    The output of the server goes to output_file.\n"
          "                    Default value: /dev/null.\n"
	  "  -l                Prefix every line of the output file\n"
	  "                    with a timestamp.\n"
	  "  -u                Do not buffer writing to the output file.\n"
	  "                    (The server might buffer its output, though.)\n"
          "  -m file1 -m file2 Moves file1 to file2 after the server quits.\n"
          "                    Several -m options can be specified;\n"
          "                    the moves are executed in order.\n"
	  "\n"
	  "Example: start_server java lse.neko.comm.Slave\n"
	  "The server should print Ready as the first line\n"
	  "of its output when it is ready to accept connections.\n"
	  "At this moment, start_server prints the following line:\n"
	  "\n"
	  "pid pid_of_server\n"
	  "\n"
	  "where pid_of_server is the process id of the server."
	  "It then returns with exit code 0.\n"
	  "Other exit codes mean unsuccessful operation.\n");
  exit(1);
}

int real_main(int argc, char* argv[]);

int main(int argc, char* argv[])
{

  int c;
  extern char *optarg;
  extern int optind;

  while ((c = getopt(argc, argv, "luo:m:")) != EOF) {
      switch (c) {
      case 'l':
	  timestamp = 1;
	  break;
      case 'u':
	  unbuffered = 1;
	  break;
      case 'o':
	  output_file = optarg;
	  break;
      case 'm':
	  if (num_move_files >= MAX_MOVE) usage();
	  move_files[num_move_files] = optarg;
	  num_move_files++;
	  break;
      default:
	  usage();
	  break;
      }
  }
  if (num_move_files % 2 == 1) usage();

/*
  printf("timestamp %d\n", timestamp);
  printf("output_file %s\n", output_file);
  printf("moves:\n");
  for (c=0; c<num_move_files; c+=2) {
      printf("  %s to %s\n", move_files[c], move_files[c+1]);
  }
  printf("args:\n");
  for (c=optind; c<argc; c++) {
      printf("  %s\n", argv[c]);
  }
*/
  
  if (argc <= optind) {
      usage();
  }

  return real_main(argc-optind, argv+optind);
}

void do_move_files() {

    int i;
    int pid;
    int status;

    for (i=0; i<num_move_files; i+=2) {
	if ((pid = fork())) {
	    while (pid != wait(&status)) {
	    }
	    if (status != 0) {
		fprintf(stderr, "Error in renaming files");
		exit(7);
	    }
	} else {
	    /* child */
	    execlp("mv", "mv", move_files[i], move_files[i+1], NULL);
	    perror("error in exec");
	}
    }	    

}

int real_main(int argc, char* argv[]) {
  
  int child_pid;
  int child2_pid;
  int port;

  child_pid = start_server(argv);

  if (!fgets(linebuf, LINEBUFSIZE, stdin)) {
    perror("Reading first line of output:");
    exit(3);
  }
  if (strcmp(OUTPUT_MESSAGE, linebuf) == 0) {
    printf("pid %d\n", child_pid);
  } else if (sscanf(linebuf, "Ready on port %d\n", &port) == 1) {
    printf("pid %d port %d\n", child_pid, port);
  } else {
    exit(1);
  }
  
  child2_pid = fork();
  if (child2_pid == 0) {
    log_output(output_file);
    do_move_files();
  } else if (child2_pid < 0) {
    perror("fork:");
    exit(6);
  }
  
  exit(0);

}

