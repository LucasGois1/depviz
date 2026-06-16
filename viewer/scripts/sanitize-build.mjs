import { readFile, writeFile } from "node:fs/promises";
import { resolve } from "node:path";

const assetsDir = process.env.npm_package_config_assetsDir ?? "../depviz-core/target/generated-resources/depviz/assets";
const appPath = resolve(assetsDir, "app.js");
const app = await readFile(appPath, "utf8");

await writeFile(appPath, app.replaceAll("https://react.dev/errors/", "react-error-"), "utf8");
