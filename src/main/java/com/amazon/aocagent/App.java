/*
 * This Java source file was generated by the Gradle 'init' task.
 */

package com.amazon.aocagent;

import com.amazon.aocagent.commands.Candidate;
import com.amazon.aocagent.commands.IntegTest;
import com.amazon.aocagent.commands.Release;
import lombok.extern.log4j.Log4j2;
import picocli.CommandLine;

@CommandLine.Command(
    name = "aoccicd",
    mixinStandardHelpOptions = true,
    version = "0.1",
    description = "use for integtests and releases of the aocagent",
    subcommands = {IntegTest.class, Release.class, Candidate.class}
)
@Log4j2
public class App implements Runnable {
  public static void main(String[] args) {
    int exitCode = new CommandLine(new App()).execute(args);
    System.exit(exitCode);
  }

  @Override
  public void run() {
    log.info("Starting");
  }
}
