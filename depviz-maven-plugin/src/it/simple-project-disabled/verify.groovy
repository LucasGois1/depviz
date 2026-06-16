import groovy.json.JsonSlurper

def output = new File(basedir, "target/depviz")
def htmlFile = new File(output, "dependency-graph.html")
def jsonFile = new File(output, "dependency-graph.json")

assert htmlFile.isFile() : "Expected generated HTML at ${htmlFile}"
assert jsonFile.isFile() : "Expected generated JSON at ${jsonFile}"

def html = htmlFile.getText("UTF-8")
assert html.contains('id="graph-root"') : "HTML should include the viewer root"
assert html.contains('id="depviz-data"') : "HTML should include embedded graph data"

def json = new JsonSlurper().parse(jsonFile)
def artifactIds = json.nodes.collect { it.artifactId } as Set
assert artifactIds.contains("simple-project-disabled") : "JSON should include the sample project node"
assert artifactIds.contains("spring-core") : "JSON should include the shared spring-core dependency node"

assert json.versionSummary.enabled == false : "Disabled generation should report version checks as disabled"
assert json.versionSummary.checked == 0 : "Disabled generation should not check dependency versions"
assert json.versionSummary.current == 0 : "Disabled generation should not report current dependency counts"
assert json.versionSummary.outdated == 0 : "Disabled generation should not report outdated dependency counts"
assert json.versionSummary.unavailable == 0 : "Disabled generation should not report unavailable dependency counts"

assert json.nodes.every { it.versionInsight == null } :
    "Nodes should not include update insight details when update checks are disabled"

def incomingByTarget = json.edges.groupBy { it.target }
def sharedNodes = json.nodes.findAll { node ->
    (incomingByTarget[node.id] ?: []).collect { it.source }.toSet().size() > 1
}
assert sharedNodes.any { it.artifactId == "spring-core" } :
    "Expected spring-core to remain one shared node with multiple incoming edges"
