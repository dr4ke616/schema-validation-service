#!/bin/bash

HOST=localhost
PORT=8080

ID=$1

echo "GET $ID"
curl -s -XGET http://$HOST:$PORT/schema/$ID | jq ''

echo ""
echo "POST $ID"
curl -s -XPOST http://$HOST:$PORT/schema/$ID -d '
{
  "$schema": "http://json-schema.org/draft-04/schema#",
  "type": "object",
  "properties": {
    "source": {
      "type": "string"
    },
    "destination": {
      "type": "string"
    },
    "timeout": {
      "type": "integer",
      "minimum": 0,
      "maximum": 32767
    },
    "chunks": {
      "type": "object",
      "properties": {
        "size": {
          "type": "integer"
        },
        "number": {
          "type": "integer"
        }
      },
      "required": ["size"]
    }
  },
  "required": ["source", "destination"]
}
' | jq ''

echo ""
echo "GET $ID"
curl -s -XGET http://$HOST:$PORT/schema/$ID | jq ''

echo ""
echo "POST $ID valid document"
curl -s -XPOST http://$HOST:$PORT/validate/$ID -d '
{
  "source": "/home/alice/image.iso",
  "destination": "/mnt/storage",
  "timeout": null,
  "chunks": {
    "size": 1024,
    "number": null
  }
}
' | jq ''

echo ""
echo "POST $ID invalid document"
curl -s -XPOST http://$HOST:$PORT/validate/$ID -d '
{
  "timeout": null,
  "chunks": {
    "size": 1024,
    "number": null
  }
}
' | jq ''