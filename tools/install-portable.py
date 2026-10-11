"""Install an included cosmetic port into an explicitly selected game directory."""
import argparse
import hashlib
import shutil
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
RELEASES = {
    "forge-1.8.9": ("badlion-cosmetic-port-forge-0.5.0.jar", "a01bf9379ab1b7b380c8dd2e2c045ea17347aa8cd53a31079539e952cb3a6dba"),
    "fabric-1.21.1": ("badlion-cosmetic-port-fabric-1.21.1-0.5.0.jar", "d6d7f4a11dca66433c56472bc91799ddceebd993c8c85e583ccdee08c4700e0c"),
    "fabric-1.20.1": ("badlion-cosmetic-port-fabric-1.20.1-0.5.0.jar", "8dde3735180121b1afe6b4b88a9d3ecdc0572e749fe6192fe955bf1258887a18"),
    "forge-1.12.2": ("badlion-cosmetic-port-forge-1.12.2-0.5.0.jar", "8c5f457c8ad6506d2d1bae7d94c0c00d16db057eb09c4ce35029378af7905d66"),
    "forge": ("badlion-cosmetic-port-forge-0.4.0.jar", "1fb35136661f642f5a9832611f256b1864a20ae6130fddcfad9c1a42f0b1eb10"),
    "fabric": ("badlion-cosmetic-port-0.3.6.jar", "8a0e0959ef6807329cc696b930b02febb432f235c4642af2da835abe67ae2177"),
}


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--target", choices=RELEASES, required=True)
    parser.add_argument("--game-dir", type=Path, required=True)
    parser.add_argument("--copy-assets", action="store_true")
    args = parser.parse_args()
    game = args.game_dir.expanduser().resolve()
    if not game.is_dir():
        parser.error("The selected game directory must already exist")
    name, expected = RELEASES[args.target]
    jar = ROOT / "release" / name
    actual = hashlib.sha256(jar.read_bytes()).hexdigest()
    if actual != expected:
        raise RuntimeError("Release JAR checksum mismatch")
    assets = ROOT / "exports/mesh-assets"
    if not (assets / "catalog.json").is_file():
        raise RuntimeError("The complete mesh-assets directory is required")
    target = game / "config/badlion-cosmetic-port/mesh-assets"
    destination = game / "mods" / name
    existing = list((game / "mods").glob("badlion-cosmetic-port*.jar"))
    if existing and (existing != [destination] or destination.read_bytes() != jar.read_bytes()):
        raise RuntimeError("An existing cosmetic port JAR is present; move it aside before installing")
    if target.exists() or target.is_symlink():
        if not target.is_symlink() or target.resolve() != assets.resolve():
            raise RuntimeError("Existing asset directory differs; move it aside before installing")
    else:
        target.parent.mkdir(parents=True, exist_ok=True)
        if args.copy_assets:
            shutil.copytree(assets, target)
        else:
            target.symlink_to(assets.resolve(), target_is_directory=True)
    destination.parent.mkdir(parents=True, exist_ok=True)
    shutil.copy2(jar, destination)
    print(f"Installed {name} in {game}")
    print("Install the matching Minecraft loader separately, then press K in game.")


if __name__ == "__main__":
    main()
