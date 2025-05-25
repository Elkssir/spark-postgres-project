/* -------------------------------------------------------------
   Tables cibles dans la base sparkdb (PostgreSQL 15)
------------------------------------------------------------- */

-- A. Table journalière brute (après filtre Maroc)
CREATE TABLE IF NOT EXISTS covid_daily_ma (
  date          DATE PRIMARY KEY,
  new_cases     INTEGER,
  new_deaths    INTEGER,
  total_cases   INTEGER,
  total_deaths  INTEGER
);

CREATE TABLE IF NOT EXISTS covid_totaux_pays (
    location       VARCHAR(255),
    total_cases    BIGINT,
    total_deaths   BIGINT
);

-- B. Table d’indicateurs mensuels (KPIs)
CREATE TABLE IF NOT EXISTS covid_kpi_ma_mois (
  mois             CHAR(7)  PRIMARY KEY,   -- ex : 2021-08
  cas_mois         INTEGER,
  deces_mois       INTEGER,
  incidence_100k   NUMERIC(8,2),
  létalité_mois    NUMERIC(6,4)            -- deaths / cases
);

-- B. Table Pour WordCount simple
CREATE TABLE IF NOT EXISTS word_counts (
    word VARCHAR(255) PRIMARY KEY,
    count INTEGER
);

