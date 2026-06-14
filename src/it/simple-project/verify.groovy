import groovy.json.JsonSlurper

def output = new File(basedir, "target/depviz")
def htmlFile = new File(output, "dependency-graph.html")
def jsonFile = new File(output, "dependency-graph.json")

assert htmlFile.isFile() : "Expected generated HTML at ${htmlFile}"
assert jsonFile.isFile() : "Expected generated JSON at ${jsonFile}"

def html = htmlFile.getText("UTF-8")
assert html.contains('id="graph-root"') : "HTML should include the viewer root"
assert html.contains('id="depviz-data"') : "HTML should include embedded graph data"
assert html.contains('href="assets/style.css"') : "HTML should reference the local stylesheet"
assert html.contains('src="assets/app.js"') : "HTML should reference the local JavaScript bundle"

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

def incomingByTarget = json.edges.groupBy { it.target }
def sharedNodes = json.nodes.findAll { node ->
    (incomingByTarget[node.id] ?: []).collect { it.source }.toSet().size() > 1
}
assert sharedNodes.any { it.artifactId == "spring-core" } :
    "Expected spring-core to be one shared node with multiple incoming edges"
