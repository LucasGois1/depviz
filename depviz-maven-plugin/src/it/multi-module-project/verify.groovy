import groovy.json.JsonSlurper

def jsonFile = new File(basedir, "target/depviz/dependency-graph.json")

assert jsonFile.isFile() : "Expected generated JSON at ${jsonFile}"

def json = new JsonSlurper().parse(jsonFile)
def root = json.nodes.find { it.root }
assert root != null : "Expected one root node"
assert root.type == "reactor" : "Expected root node type reactor, got ${root.type}"

def api = json.nodes.find { it.artifactId == "api" && it.moduleRoot }
def worker = json.nodes.find { it.artifactId == "worker" && it.moduleRoot }
assert api != null : "Expected api moduleRoot node"
assert worker != null : "Expected worker moduleRoot node"

def slf4jNodes = json.nodes.findAll { it.groupId == "org.slf4j" && it.artifactId == "slf4j-api" }
assert slf4jNodes.size() == 1 : "Expected one deduplicated slf4j-api graph node, got ${slf4jNodes.size()}"

def slf4j = slf4jNodes[0]
def incoming = json.edges.findAll { it.target == slf4j.id }
assert incoming.size() == 2 : "Expected exactly two incoming edges to slf4j-api, got ${incoming.size()}"
assert (incoming.collect { it.source } as Set) == ([api.id, worker.id] as Set) :
    "Expected slf4j-api incoming edges from api and worker module nodes"
