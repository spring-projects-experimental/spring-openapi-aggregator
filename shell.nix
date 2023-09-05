with import <nixpkgs> { };

mkShell {
  name = "env";
  buildInputs = [ nodejs ];
  shellHook = ''
    export PATH=$PWD/node_modules/.bin:$PATH
  '';
}
