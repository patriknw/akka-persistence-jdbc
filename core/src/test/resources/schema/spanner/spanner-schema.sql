DROP TABLE journal;

CREATE TABLE journal (
  write_time TIMESTAMP OPTIONS (allow_commit_timestamp=true),
  ordering INT64,
  persistence_id STRING(255) NOT NULL,
  sequence_number INT64 NOT NULL,
  deleted BOOL,
  tags STRING(255),
  message BYTES(MAX) NOT NULL)
  PRIMARY KEY (persistence_id, sequence_number);

DROP TABLE snapshot;

CREATE TABLE snapshot (
  persistence_id STRING(255) NOT NULL,
  sequence_number INT64 NOT NULL,
  created INT64 NOT NULL,
  snapshot BYTES(MAX) NOT NULL)
  PRIMARY KEY (persistence_id, sequence_number);

