#!/bin/bash

sed -i "s|DEVTOOLS_URL_PLACEHOLDER|wss://${CODESPACE_NAME}-9630.app.github.dev|g" shadow-cljs.edn