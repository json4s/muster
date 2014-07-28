#!/bin/sh

cp $TRAVIS_BUILD_DIR/README.md $TRAVIS_BUILD_DIR/src/jekyll/_includes/content.md
cd $TRAVIS_BUILD_DIR/src/jekyll
bundle install 