package com.example;

import org.apache.spark.sql.*;
import static org.apache.spark.sql.functions.*;

import java.util.Properties;

public class MarocCovidAnalysisApp {

  // 0) paramètres génériques
  private static final String JDBC_URL ="jdbc:postgresql://postgres:5432/sparkdb";

  private static final Properties DB_PROPS = new Properties();
  
  static { DB_PROPS.put("user","spark"); DB_PROPS.put("password","spark123"); DB_PROPS.put("driver", "org.postgresql.Driver");}

  public static void main(String[] args) {

    /* 1) Session Spark --------------------------------------------------- */
    SparkSession spark = SparkSession.builder()
        .appName("Analyse COVID Maroc (Spark ⇒ PostgreSQL)")
        .getOrCreate();

    /* 2) Lire le CSV global, filtrer MAR --------------------------------- */
    Dataset<Row> daily = spark.read()
        .option("header","true")
        .option("inferSchema","true")
        .csv("/opt/data/owid-covid-data.csv")     // volume partagé
        .filter(col("code").equalTo("MAR"))
        .select(
            col("date").cast("date"),
            col("new_cases").cast("int"),
            col("new_deaths").cast("int"),
            col("total_cases").cast("int"),
            col("total_deaths").cast("int"),
            col("population").cast("int")         // une seule valeur
        );

    /* 3) Sauvegarde brute ------------------------------------------------ */
    daily.write()
         .mode("overwrite")
         .jdbc(JDBC_URL, "covid_daily_ma", DB_PROPS);

    /* 4) Calculs mensuels (KPIs) ---------------------------------------- */
    Dataset<Row> kpi = daily
        .withColumn("mois", date_format(col("date"),"yyyy-MM"))
        .groupBy("mois")
        .agg(
            sum("new_cases").alias("cas_mois"),
            sum("new_deaths").alias("deces_mois"),
            first("population").alias("pop")      // 36 M hab. ≈ constant
        )
        .withColumn("incidence_100k",
            col("cas_mois").multiply(100_000).divide(col("pop")))
        .withColumn("létalité_mois",
            col("deces_mois").divide(col("cas_mois")))
        .select("mois","cas_mois","deces_mois",
                "incidence_100k","létalité_mois")
        .orderBy("mois");

    /* 5) Persistance KPI ------------------------------------------------- */
    kpi.write()
       .mode("overwrite")
       .jdbc(JDBC_URL, "covid_kpi_ma_mois", DB_PROPS);

    System.out.println("✅ Données journalières + KPI mensuels insérés !");

    //  Ajout : total des cas/décès par pays
    Dataset<Row> totauxParPays = spark.read()
    .option("header", "true")
    .option("inferSchema", "true")
    .csv("/opt/data/owid-covid-data.csv")
    .filter(col("total_cases").isNotNull().and(col("total_deaths").isNotNull()))
    .groupBy(col("country"))
    .agg(
        max("total_cases").alias("total_cases"),
        max("total_deaths").alias("total_deaths")
    )
    .orderBy(col("total_cases").desc());

// Enregistrement dans PostgreSQL
totauxParPays.write()
    .mode("overwrite")
    .jdbc(JDBC_URL, "covid_totaux_pays", DB_PROPS);

System.out.println("✅ Totaux par pays insérés dans la base.");

    spark.stop();
  }
}
