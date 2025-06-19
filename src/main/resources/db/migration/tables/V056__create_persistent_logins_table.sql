-- V056__create_persistent_logins_table.sql
-- Spring Security's persistent login (remember-me) table

CREATE TABLE persistent_logins
(
    series              VARCHAR(64) PRIMARY KEY,
    username            VARCHAR(64) NOT NULL, -- email of customer account
    token               VARCHAR(64) NOT NULL,
    last_used           TIMESTAMP   NOT NULL
);