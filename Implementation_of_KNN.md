# Реализация KNN с помощью процедур MySQL

Обработка датасета - применение k средних к датасету
Столбцы :
Country name(в таблице `country`)

Нужные столбцы для разбиения на категории (будут использоваться в алгоритме k means):
 - Ladder score (в таблице `ladder_score` )
 - Logged GDP per capita (в таблице `logged_GDP_per_capita`)
 - Social support (в таблице `social_support`)
 - Healthy life expectancy (в таблице `healthy_life_expectancy`)
 - Freedom to make life choices (в таблице `freedom_to_make_life_choices`)
 - Perceptions of corruption (в таблице `perceptions_of_corruption`)


Нормализую необходимые столбцы

Проверка, что все нужные столбцы > 0.:
```
select *
from happiness
where
ladder_score <= 0 or
logged_GDP_per_capita <= 0 or
social_support <= 0 or
healthy_life_expectancy <= 0 or
freedom_to_make_life_choices <= 0 or
perceptions_of_corruption <= 0;
```
Получилась пустая таблица.


Заменим имя изначальной таблицы на новое, для того чтобы создать новую с нормализованными столбцами.  
```rename table happiness to original_happiness;```

Создадим таблицу min_max_columns_original_happiness, содержащую максимальные и минимальные значения столбцов из original_happiness:

```
create table min_max_columns_original_happiness
select
min(ladder_score) as min_ladder_score,
min(logged_GDP_per_capita) as min_logged_GDP_per_capita,
min(social_support) as min_social_support,
min(healthy_life_expectancy) as min_healthy_life_expectancy,
min(freedom_to_make_life_choices) as min_freedom_to_make_life_choices,
min(perceptions_of_corruption) as min_perceptions_of_corruption,
min(ladder_score_in_Dystopia)  as min_ladder_score_in_Dystopia,
max(ladder_score) as max_ladder_score,
max(logged_GDP_per_capita) as max_logged_GDP_per_capita,
max(social_support) as max_social_support,
max(healthy_life_expectancy) as max_healthy_life_expectancy,
max(freedom_to_make_life_choices) as max_freedom_to_make_life_choices,
max(perceptions_of_corruption) as max_perceptions_of_corruption
from original_happiness;
```

Создадим таблицу happiness со столбцами original_happiness к которыми применим min-max нормализацию:
```
create table happiness
select id,
country,
(ladder_score - min_ladder_score) / (max_ladder_score - min_ladder_score) as ladder_score,
(logged_GDP_per_capita - min_logged_GDP_per_capita) / (max_logged_GDP_per_capita - min_logged_GDP_per_capita) as logged_GDP_per_capita,
(social_support - min_social_support) / (max_social_support - min_social_support) as social_support,
(healthy_life_expectancy - min_healthy_life_expectancy) / (max_healthy_life_expectancy - min_healthy_life_expectancy) as healthy_life_expectancy,
(freedom_to_make_life_choices - min_freedom_to_make_life_choices) / (max_freedom_to_make_life_choices - min_freedom_to_make_life_choices) as freedom_to_make_life_choices,
(perceptions_of_corruption - min_perceptions_of_corruption) / (max_perceptions_of_corruption - min_perceptions_of_corruption) as perceptions_of_corruption
from original_happiness, min_max_columns_original_happiness;
```

### Алгоритм K-means состоит из трех процедур:
#### KMeansClustering
инициализация центроид через рандомную выборку из датасета
loop в которой вызывались процедуры distance и new_coord_centroids
создание таблицы country_label в которой столбцы: country, label  
distance
создание таблицы distance в которой находиться расстояние от каждого центроида до каждой страны
создание таблицы country_centroid на оснавании distance  в ней содержиться страна центроид до которого наименьшее расстояние и само расстояние
new_coord_centroids
нахождение новых координат центроидов
проверка что новые координаты отличаются от предыдущих на заданное accuracy

##### Код KMeansClustering:

```
CREATE DEFINER=`root`@`localhost` PROCEDURE `KMeansClustering`(
IN k INT  
)
BEGIN
DECLARE max_steps INT; /* МАКСИМАЛЬНОЕ КОЛИЧЕСТВО ПОВТОРЕНИЙ */
DECLARE accuracy INT; /* ТОЧНОСТЬ ОТВЕТА */
DECLARE flag BOOLEAN;

    SET max_steps = 50;
    SET accuracy = 0.0001;
    SET flag = 1;
    
    DROP TABLE IF EXISTS centroids;
    
    CREATE TABLE centroids
    SELECT ladder_score, 
		perceptions_of_corruption, 
        freedom_to_make_life_choices,
        healthy_life_expectancy,
        social_support,
        logged_GDP_per_capita
	FROM happiness
	ORDER BY RAND()
	LIMIT k;
    
    ALTER TABLE centroids ADD id INT PRIMARY KEY AUTO_INCREMENT;
    
    WHILE max_steps > 0 AND flag  = 1 DO
        CALL distance(k);
        CALL new_coord_centroids(k, accuracy, @count_accuracy);
        IF @count_accuracy = k THEN
            SET flag = 0;
        END IF;
        SET max_steps = max_steps - 1;
    END WHILE;
    
    DROP TABLE IF EXISTS country_label;
    
    CREATE TABLE country_label
    SELECT 
		country,
        centroid_id AS label
    FROM happiness
    JOIN country_centroid
    ON happiness.id = country_centroid.country_id;
    
    DROP TABLE country_centroid;

END
```

Код distance:
```
CREATE DEFINER=`root`@`localhost` PROCEDURE `distance`(
IN k INT
)
BEGIN

    DROP TABLE IF EXISTS distance;  
    CREATE TABLE distance 
    SELECT a.id AS country_id, b.id AS centroid_id, 
    SQRT(
    POW(a.ladder_score - b.ladder_score, 2) 
    + POW(a.perceptions_of_corruption - b.perceptions_of_corruption, 2)
    + POW(a.freedom_to_make_life_choices - b.freedom_to_make_life_choices, 2)
    + POW(a.healthy_life_expectancy - b.healthy_life_expectancy, 2)
    + POW(a.social_support - b.social_support, 2)
    + POW(a.logged_GDP_per_capita - b.logged_GDP_per_capita, 2)
    ) AS dist
    FROM happiness AS a
    CROSS JOIN centroids AS b;
    
    DROP TABLE IF EXISTS country_centroid;

    CREATE TABLE country_centroid
    SELECT country_id, centroid_id, dist
    FROM distance a 
    WHERE dist = (SELECT MIN(dist) FROM distance b WHERE a.country_id = b.country_id);
    
    DROP TABLE distance;

END
```

Код new_coord_centroids:
```
CREATE DEFINER=`root`@`localhost` PROCEDURE `new_coord_centroids`(
IN k INT,
IN accuracy INT,
OUT count_accuracy INT
)
BEGIN
DECLARE new_ladder_score DOUBLE;
DECLARE new_perceptions_of_corruption DOUBLE;
DECLARE new_freedom_to_make_life_choices DOUBLE;
DECLARE new_healthy_life_expectancy DOUBLE;
DECLARE new_social_support DOUBLE;
DECLARE new_logged_GDP_per_capita DOUBLE;

    DECLARE ladder DOUBLE; 
    DECLARE corruption DOUBLE; 
    DECLARE freedom DOUBLE; 
    DECLARE expectancy DOUBLE; 
    DECLARE support DOUBLE; 
    DECLARE GDP DOUBLE; 
    
    DECLARE dist DOUBLE;
    DECLARE count_accuracy INT DEFAULT 0;
    DECLARE n INT;

SET n = 1;

    WHILE n <= k DO
		SELECT AVG(ladder_score) INTO new_ladder_score
			FROM happiness 
            WHERE id IN (SELECT country_id FROM country_centroid WHERE centroid_id = n);
        
		SELECT AVG(perceptions_of_corruption) INTO new_perceptions_of_corruption
			FROM happiness 
            WHERE id IN (SELECT country_id FROM country_centroid WHERE centroid_id = n);
            
		SELECT AVG(freedom_to_make_life_choices) INTO new_freedom_to_make_life_choices
			FROM happiness 
            WHERE id IN (SELECT country_id FROM country_centroid WHERE centroid_id = n);
		
        SELECT AVG(healthy_life_expectancy) INTO new_healthy_life_expectancy
			FROM happiness 
            WHERE id IN (SELECT country_id FROM country_centroid WHERE centroid_id = n);
		
        SELECT AVG(social_support) INTO new_social_support
			FROM happiness 
            WHERE id IN (SELECT country_id FROM country_centroid WHERE centroid_id = n);
		
        SELECT AVG(logged_GDP_per_capita) INTO new_logged_GDP_per_capita
			FROM happiness 
            WHERE id IN (SELECT country_id FROM country_centroid WHERE centroid_id = n);
       
        SELECT ladder_score INTO ladder FROM centroids WHERE id = @n;
        SELECT perceptions_of_corruption INTO corruption FROM centroids WHERE id = @n;
        SELECT freedom_to_make_life_choices INTO freedom FROM centroids WHERE id = @n;
        SELECT healthy_life_expectancy INTO expectancy FROM centroids WHERE id = @n;
        SELECT social_support INTO support FROM centroids WHERE id = @n;
        SELECT logged_GDP_per_capita INTO GDP FROM centroids WHERE id = @n;
        
        SET dist = SQRT(
			POW(new_ladder_score - ladder, 2) 
			+ POW(new_perceptions_of_corruption - corruption, 2)
			+ POW(new_freedom_to_make_life_choices - freedom, 2)
			+ POW(new_healthy_life_expectancy - expectancy, 2)
			+ POW(new_social_support - support, 2)
			+ POW(new_logged_GDP_per_capita - GDP, 2)
			);
        
        IF (dist <= accuracy) 
			OR (new_ladder_score = 0) 
            OR (new_perceptions_of_corruption = 0) 
            OR (new_freedom_to_make_life_choices = 0)
            OR (new_healthy_life_expectancy = 0)
            OR (new_social_support = 0)
            OR (new_logged_GDP_per_capita = 0)
            THEN
			SET count_accuracy = count_accuracy + 1;
        END IF;
        
        IF (new_ladder_score != 0) 
			AND (new_perceptions_of_corruption != 0) 
            AND (new_freedom_to_make_life_choices != 0) 
            AND (new_healthy_life_expectancy != 0) 
            AND (new_social_support != 0) 
            AND (new_logged_GDP_per_capita != 0)
            THEN
			UPDATE centroids 
            SET 
            ladder_score = new_ladder_score, 
            perceptions_of_corruption = new_perceptions_of_corruption,
            freedom_to_make_life_choices = new_freedom_to_make_life_choices, 
            healthy_life_expectancy = new_healthy_life_expectancy,
            social_support = new_social_support,
            logged_GDP_per_capita = new_logged_GDP_per_capita
            WHERE id = n;
		END IF;
        
        SET n = n + 1;
    END WHILE;
END
```


### После применения K средних к датасету:
Получаем две новые таблицы:
 - country_label 
   - Столбцы: country, label
 - centroids - информация о центроидах 
