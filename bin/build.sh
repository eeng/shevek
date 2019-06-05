#!/bin/bash

# Script to build the jar file and generate the release tarball to upload

set -o errexit -o nounset

version=`head -n1 project.clj | cut -d ' ' -f 3 | cut -d '"' -f 2`
package_name=shevek-$version
package_dir=dist/$package_name
package_tgz=$package_dir.tgz

echo "Building Shevek v$version to $package_tgz ..."

rm -rf dist
mkdir -p $package_dir

lein package

cp target/uberjar/shevek.jar $package_dir
cp resources/config.edn $package_dir

tar czf $package_tgz -C dist $package_name

echo "Done!"
