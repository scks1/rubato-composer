# MusicXML 4.0 schema (test data)

MusicXMLTest validates the writer's output against these files on every Maven build. They are test data: no Rubato jar includes them.

Source: the `musicxml-4.0.zip` asset of the w3c/musicxml v4.0 release (zip sha256 4c815790c6cdcceef6b5c6a3c17b7229f52c2754eb31b61a63d30f60ef41acbc), files `schema/musicxml.xsd`, `schema/xml.xsd` and `schema/xlink.xsd`, copied unchanged.

| File | sha256 |
|---|---|
| musicxml.xsd | bfe37ed25a9ec00e6f2591d53df260b84efe12aed209ba3ac0a76f9287665a99 |
| xlink.xsd | 6e601f8eeb41618b50e4c7f944dff754e57ea43b602755470dda24c9c2f6df92 |
| xml.xsd | 616a3077df5cfc954ac74a75abe9697b95eef7a85dbe09367d995a483e840eb5 |

Licenses:

- `musicxml.xsd` and `xlink.xsd`: copyright 2004-2021 the Contributors to the MusicXML Specification, published by the W3C Music Notation Community Group under the W3C Community Final Specification Agreement (https://www.w3.org/community/about/agreements/final/). Each file's header states this.
- `xml.xsd`: the W3C schema document for the XML namespace, as shipped in the same release. The file carries no copyright or license statement.

`musicxml.xsd` imports the other two by their musicxml.org URLs. The test resolves those imports to the files in this directory and never fetches them.
