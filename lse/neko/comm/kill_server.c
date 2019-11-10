#include <stdio.h>
#include <errno.h>
#include <unistd.h>
#include <signal.h>
#include <stdlib.h>

#define AMOUNT_OF_SLEEP 1

/* Function that ensures that the server
 * with the given pid is killed.
 */
void kill_server(pid_t child) {
    
    static int signals[] = { 15, 1, 2, 9, 0 };
    int *next_signal = signals;
    
    while (1) {
	if (0 != kill(child, *next_signal)) {
	    if (errno == ESRCH) {
		return;
	    }
	    perror("Error in kill: ");
	}
	if (*next_signal > 0) {
	    next_signal++;
	}
	sleep(AMOUNT_OF_SLEEP);
    }

}

void usage()
{
  fprintf(stderr,
	  "Usage: kill_server pid\n\n"
	  "Makes an effort at killing the process with id pid.\n"
	  "It returns with exit code 0 if it could kill the process\n"
	  "(or the process exited before).\n"
	  "Other exit codes mean unsuccessful operation.\n");
  exit(1);
}


int main(int argc, char* argv[])
{

  int child_pid;

  if (argc != 2) {
      usage();
  }
  child_pid = atoi(argv[1]);
  if (child_pid <= 0) {
      usage();
  }

  kill_server(child_pid);
  exit(0);

}

