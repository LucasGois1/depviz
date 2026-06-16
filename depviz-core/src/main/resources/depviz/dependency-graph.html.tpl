<!doctype html>
<html lang="en">
<head>
  <meta charset="utf-8">
  <meta name="viewport" content="width=device-width, initial-scale=1">
  <title>Depviz Dependency Graph</title>
  <link rel="stylesheet" href="{{STYLE_ASSET}}">
</head>
<body>
  <main id="graph-root" class="viewer-shell">
    <header class="viewer-header">
      <div>
        <p class="eyebrow">Depviz</p>
        <h1>Dependency Graph</h1>
      </div>
      <dl class="summary-strip" aria-label="Graph summary">
        <div>
          <dt>Nodes</dt>
          <dd id="node-count">0</dd>
        </div>
        <div>
          <dt>Edges</dt>
          <dd id="edge-count">0</dd>
        </div>
      </dl>
    </header>

    <section class="graph-panel" aria-label="Dependency graph">
      <div id="graph-canvas" class="graph-canvas"></div>
    </section>
  </main>

  <script id="depviz-data" type="application/json">{{DEPVIZ_DATA}}</script>
  <script src="{{APP_ASSET}}"></script>
</body>
</html>
