#!/bin/bash
cd "$(dirname "$0")"
echo "Testing UI..."
npm install
npm test
