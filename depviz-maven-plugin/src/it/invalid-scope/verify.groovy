def log = new File(basedir, "build.log")

assert log.isFile() : "Expected invoker build log at ${log}"
assert log.getText("UTF-8").contains("Invalid depviz.scope 'production'") :
    "Expected invalid scope message in ${log}"
