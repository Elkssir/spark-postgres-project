package com.example;

import org.apache.spark.SparkConf;
import org.apache.spark.api.java.JavaPairRDD;
import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.api.java.JavaSparkContext;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SparkSession;
import scala.Tuple2;

import java.util.Arrays;
import java.util.Properties;

public class WordCountApp {
    public static void main(String[] args) {
        // Configuration Spark
        SparkConf conf = new SparkConf()
                .setAppName("Word Count Application")
                .setMaster("spark://spark-master:7077");
        
        // Création du contexte Spark
        JavaSparkContext sc = new JavaSparkContext(conf);
        
        try {
            // Création de la session Spark
            SparkSession spark = SparkSession.builder()
                    .config(conf)
                    .getOrCreate();
            
            // Chemin du fichier texte
            String filePath = "/opt/data/sample-text.txt";
            
            // Lecture du fichier texte
            JavaRDD<String> lines = sc.textFile(filePath);
            
            // Traitement pour compter les mots
            JavaRDD<String> words = lines.flatMap(line -> Arrays.asList(line.toLowerCase().split("\\W+")).iterator());
            JavaPairRDD<String, Integer> wordCounts = words
                    .filter(word -> !word.isEmpty())
                    .mapToPair(word -> new Tuple2<>(word, 1))
                    .reduceByKey((a, b) -> a + b);
            
            // Conversion en DataFrame
            Dataset<Row> wordCountsDF = spark.createDataFrame(
                    wordCounts.map(tuple -> RowFactory.create(tuple._1, tuple._2)),
                    createStructType()
            );
            
            // Affichage des résultats
            System.out.println("Résultats du comptage de mots:");
            wordCountsDF.show();
            
            // Configuration de la connexion PostgreSQL
            String url = "jdbc:postgresql://postgres:5432/sparkdb";
            Properties connectionProperties = new Properties();
            connectionProperties.put("user", "spark");
            connectionProperties.put("password", "spark123");
            connectionProperties.put("driver", "org.postgresql.Driver");
            
            // Sauvegarde des résultats dans PostgreSQL
            wordCountsDF
                    .write()
                    .mode("overwrite")
                    .jdbc(url, "word_counts", connectionProperties);
            
            System.out.println("Les résultats ont été sauvegardés dans PostgreSQL avec succès!");
            
        } catch (Exception e) {
            System.err.println("Erreur pendant l'exécution du job: " + e.getMessage());
            e.printStackTrace();
        } finally {
            sc.close();
        }
    }
    
    private static org.apache.spark.sql.types.StructType createStructType() {
        return new org.apache.spark.sql.types.StructType()
                .add("word", org.apache.spark.sql.types.DataTypes.StringType)
                .add("count", org.apache.spark.sql.types.DataTypes.IntegerType);
    }
    
    private static class RowFactory {
        public static Row create(Object... values) {
            return org.apache.spark.sql.RowFactory.create(values);
        }
    }
}