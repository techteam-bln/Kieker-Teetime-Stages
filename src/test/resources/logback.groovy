import ch.qos.logback.classic.filter.ThresholdFilter

statusListener(OnConsoleStatusListener)

appender("FILE", FileAppender) {
  file = "src/test/data/load-logs/timings-results.txt"
  append = false
  filter(ThresholdFilter) {
	level = INFO
  }
  encoder(PatternLayoutEncoder) {
    pattern = "%msg%n"
  }
}

appender("CONSOLE", ConsoleAppender) {
  encoder(PatternLayoutEncoder) {
    pattern = "%d{HH:mm:ss.SSS} %level %logger - %msg%n"
  }
}

root WARN, ["CONSOLE"]

//logger "teetime.framework", INFO
//logger "teetime.framework.signal", INFO
//logger "teetime.stage", INFO

logger "util", INFO