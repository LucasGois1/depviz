import groovy.json.JsonSlurper

def jsonFile = new File(basedir, "target/depviz/dependency-graph.json")

assert jsonFile.isFile() : "Expected generated JSON at ${jsonFile}"

def json = new JsonSlurper().parse(jsonFile)
assert json.securitySummary.enabled == true : "Snyk JSON generation should enable security summary"
assert json.securitySummary.checked == true : "Snyk JSON generation should mark security as checked"
assert json.securitySummary.vulnerableNodes == 1 : "Expected one vulnerable graph node"
assert json.securitySummary.affectedModules == 0 : "Expected no affected module roots in single-module fixture"
assert json.securitySummary.critical == 0 : "Expected no critical severity Snyk findings"
assert json.securitySummary.high == 1 : "Expected one high severity Snyk finding"
assert json.securitySummary.medium == 0 : "Expected no medium severity Snyk findings"
assert json.securitySummary.low == 0 : "Expected no low severity Snyk findings"
assert json.securitySummary.unmappedFindings == 0 : "Expected all Snyk findings to map to graph nodes"

def slf4j = json.nodes.find { it.groupId == "org.slf4j" && it.artifactId == "slf4j-api" }
assert slf4j != null : "Expected slf4j-api dependency node"
assert slf4j.securityInsight != null : "Expected slf4j-api node to include security insight"
assert slf4j.securityInsight.maxSeverity == "high" : "Expected slf4j-api max security severity high"

def finding = slf4j.securityInsight.findings.find { it.id == "SNYK-JAVA-ORGSLF4J-TEST-1" }
assert finding != null : "Expected slf4j-api security insight to include the Snyk finding id"
assert finding.fixedVersions == ["2.0.17"] : "Expected Snyk finding fixedIn version 2.0.17"
