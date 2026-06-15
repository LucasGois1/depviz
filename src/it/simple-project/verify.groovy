import groovy.json.JsonSlurper

def output = new File(basedir, "target/depviz")
def htmlFile = new File(output, "dependency-graph.html")
def jsonFile = new File(output, "dependency-graph.json")

assert htmlFile.isFile() : "Expected generated HTML at ${htmlFile}"
assert jsonFile.isFile() : "Expected generated JSON at ${jsonFile}"

def html = htmlFile.getText("UTF-8")
assert html.contains('id="graph-root"') : "HTML should include the viewer root"
assert html.contains('id="depviz-data"') : "HTML should include embedded graph data"
assert html ==~ /(?s).*href="assets\/style\.css\?v=[a-f0-9]{12}".*/ : "HTML should reference the cache-busted local stylesheet"
assert html ==~ /(?s).*src="assets\/app\.js\?v=[a-f0-9]{12}".*/ : "HTML should reference the cache-busted local JavaScript bundle"

def externalRuntimeReference = ~/(?i)\b(?:src|href)\s*=\s*["']https?:\/\//
assert !externalRuntimeReference.matcher(html).find() : "HTML should not load runtime assets over HTTP(S)"
def externalCssUrl = ~/(?i)url\(\s*["']?https?:\/\//
assert !externalCssUrl.matcher(html).find() : "HTML should not load CSS assets over HTTP(S)"

def json = new JsonSlurper().parse(jsonFile)
def artifactIds = json.nodes.collect { it.artifactId } as Set
assert artifactIds.contains("simple-project") : "JSON should include the sample project node"
assert artifactIds.contains("spring-webmvc") : "JSON should include the spring-webmvc dependency node"
assert artifactIds.contains("spring-jdbc") : "JSON should include the spring-jdbc dependency node"
assert artifactIds.contains("spring-core") : "JSON should include the shared spring-core dependency node"

assert json.versionSummary.enabled == true : "Default generation should enable version update metadata"
assert json.versionSummary.checked > 0 : "Default generation should check at least one dependency version"

def dependencyNodes = json.nodes.findAll { !it.root }
assert dependencyNodes : "Expected dependency nodes beyond the root project"
assert dependencyNodes.every { it.versionInsight != null } :
    "Dependency nodes should include version insight metadata when update checks are enabled"

def statuses = dependencyNodes.collect { it.versionInsight.status } as Set
assert statuses.any { it in ["current", "outdated", "unavailable"] } :
    "Expected at least one resolved or unavailable version insight status"

def incomingByTarget = json.edges.groupBy { it.target }
def sharedNodes = json.nodes.findAll { node ->
    (incomingByTarget[node.id] ?: []).collect { it.source }.toSet().size() > 1
}
assert sharedNodes.any { it.artifactId == "spring-core" } :
    "Expected spring-core to be one shared node with multiple incoming edges"
