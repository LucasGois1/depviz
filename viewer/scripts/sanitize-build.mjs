import { readFile, writeFile } from "node:fs/promises";
import { resolve } from "node:path";

const appPath = resolve("../target/generated-resources/depviz/assets/app.js");
const app = await readFile(appPath, "utf8");

await writeFile(appPath, app.replaceAll("https://react.dev/errors/", "react-error-"), "utf8");
