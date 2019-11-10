package lse.neko.failureDetectors;

public interface FailureDetectorListener {

    void statusChange(boolean suspected, int p);

}

