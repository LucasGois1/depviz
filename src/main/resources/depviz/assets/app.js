(function () {
  "use strict";

  function readDocument() {
    var dataElement = document.getElementById("depviz-data");
    if (!dataElement) {
      return null;
    }
    return JSON.parse(dataElement.textContent || "{}");
  }

  function setText(id, value) {
    var element = document.getElementById(id);
    if (element) {
      element.textContent = String(value);
    }
  }

  function renderList(graph) {
    var canvas = document.getElementById("graph-canvas");
    if (!canvas) {
      return;
    }
    var nodes = graph.nodes || [];
    var list = document.createElement("ol");
    list.className = "node-list";

    nodes.forEach(function (node) {
      var item = document.createElement("li");
      item.className = "node-item";
      item.style.setProperty("--depth", String(Math.min(node.depth || 0, 8)));

      var label = document.createElement("strong");
      label.textContent = node.label || node.id;

      var coordinate = document.createElement("span");
      coordinate.textContent = node.coordinate || "";

      item.appendChild(label);
      item.appendChild(coordinate);
      list.appendChild(item);
    });

    canvas.replaceChildren(list);
  }

  var graph = readDocument();
  if (!graph) {
    return;
  }

  setText("node-count", graph.summary ? graph.summary.nodeCount : 0);
  setText("edge-count", graph.summary ? graph.summary.edgeCount : 0);
  renderList(graph);
}());
