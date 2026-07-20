const fs = require("fs");
const path = require("path");
const sharp = require("sharp");

const ICON_DIR = path.join(__dirname, "..", "icon");
const BACKUP_DIR = path.join(ICON_DIR, "originals");
const APP_DRAWABLE = path.join(__dirname, "..", "app", "src", "main", "res", "drawable");
const DS_DRAWABLE = path.join(__dirname, "..", "core", "designsystem", "src", "main", "res", "drawable");
const MIPMAP = path.join(__dirname, "..", "app", "src", "main", "res", "mipmap-xxxhdpi");

const TARGETS = {
  "adaptive_background.png": { size: 432, palette: true },
  "adaptive_foreground.png": { size: 432, palette: true },
  "foreground.png": { size: 432, palette: true },
  "monochrome.png": { size: 432, palette: true },
  "icon.png": { size: 512, palette: true },
  "logo.png": { size: 512, palette: true },
  "app_icon.png": { size: 512, palette: true },
};

async function compressFile(name, options) {
  const input = path.join(ICON_DIR, name);
  const temp = path.join(ICON_DIR, `.tmp_${name}`);
  const before = fs.statSync(input).size;

  let pipeline = sharp(input).resize(options.size, options.size, {
    fit: "contain",
    background: { r: 0, g: 0, b: 0, alpha: 0 },
  });

  pipeline = pipeline.png({
    quality: 82,
    compressionLevel: 9,
    effort: 10,
    palette: options.palette,
  });

  await pipeline.toFile(temp);
  fs.renameSync(temp, input);
  const after = fs.statSync(input).size;
  const saved = ((1 - after / before) * 100).toFixed(1);
  console.log(`${name}: ${(before / 1024).toFixed(0)}KB -> ${(after / 1024).toFixed(0)}KB (${saved}% smaller)`);
}

function copyIfExists(from, to) {
  if (fs.existsSync(from)) {
    fs.copyFileSync(from, to);
  }
}

async function syncToProject() {
  const map = {
    "adaptive_background.png": "ic_launcher_background.png",
    "adaptive_foreground.png": "ic_launcher_foreground.png",
    "monochrome.png": "ic_launcher_monochrome.png",
    "logo.png": "logo.png",
    "icon.png": "app_logo.png",
  };

  for (const [src, dest] of Object.entries(map)) {
    copyIfExists(path.join(ICON_DIR, src), path.join(APP_DRAWABLE, dest));
    if (dest === "logo.png" || dest === "app_logo.png") {
      copyIfExists(path.join(ICON_DIR, src), path.join(DS_DRAWABLE, dest));
    }
  }

  copyIfExists(path.join(ICON_DIR, "app_icon.png"), path.join(MIPMAP, "ic_launcher.png"));
  copyIfExists(path.join(ICON_DIR, "app_icon.png"), path.join(MIPMAP, "ic_launcher_round.png"));
}

async function main() {
  fs.mkdirSync(BACKUP_DIR, { recursive: true });

  for (const file of fs.readdirSync(ICON_DIR).filter((f) => f.endsWith(".png"))) {
    const src = path.join(ICON_DIR, file);
    const backup = path.join(BACKUP_DIR, file);
    if (!fs.existsSync(backup)) {
      fs.copyFileSync(src, backup);
    }
  }

  for (const [name, options] of Object.entries(TARGETS)) {
    await compressFile(name, options);
  }

  await syncToProject();
  console.log("Done. Originals backed up to icon/originals/");
}

main().catch((err) => {
  console.error(err);
  process.exit(1);
});
