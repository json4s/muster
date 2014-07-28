#!/bin/sh

cd $TRAVIS_BUILD_DIR/target/ghpages
git config user.name "${GIT_NAME}"
git config user.email "${GIT_EMAIL}"
git add .
git commit -a -m "Travis deploy to Github Pages"
git push --quiet "https://${GH_TOKEN}@${GH_REF}"  > /dev/null 2>&1
