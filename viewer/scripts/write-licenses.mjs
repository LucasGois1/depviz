import { readFile, writeFile } from "node:fs/promises";
import { resolve } from "node:path";

const packageJson = JSON.parse(await readFile(resolve("package.json"), "utf8"));
const packageLock = JSON.parse(await readFile(resolve("package-lock.json"), "utf8"));
const dependencyNames = [
  ...Object.keys(packageJson.dependencies ?? {}),
  ...Object.keys(packageJson.devDependencies ?? {})
].sort((left, right) => left.localeCompare(right));

const lines = [
  "Depviz viewer bundled dependency notices",
  "",
  "This offline viewer bundles the npm packages listed below into app.js/style.css.",
  "License metadata is sourced from package-lock.json at build time.",
  ""
];

for (const dependencyName of dependencyNames) {
  const packageInfo = packageLock.packages?.[`node_modules/${dependencyName}`];
  lines.push(`${dependencyName}@${packageInfo?.version ?? "unknown"} - ${packageInfo?.license ?? "license metadata unavailable"}`);
}

await writeFile(resolve("../src/main/resources/depviz/assets/LICENSES.txt"), `${lines.join("\n")}\n`, "utf8");
