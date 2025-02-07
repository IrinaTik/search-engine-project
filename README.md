# <h1 align="center">Поисковый движок</h1>

***
<b>*Стэк технологий*</b>:
*Java (Core, Collections, Stream), Spring Boot, Spring Boot Data JPA, Maven, 
Lombok, JSOUP, Lucene Morphology Library, MySQL, Mockito, JUnit, Log4j2.*
***

Проект реализует REST API для индексации (создания поискового индекса)
и поиска по заданным сайтам.

Основной функционал:
* Индексация сайтов, перечисленных в конфигурационном файле
* Сохранение результатов индексации в БД
* Выдача статистики по сайтам
* Поиск информации по сайтам с использованием созданного поискового индекса

## Спецификация API
В проекте представлены следующие конечные точки API:
* `GET /api/startIndexing` - запускает полную индексацию всех сайтов
или полную переиндексацию, если они уже проиндексированы
* `GET /api/stopIndexing` - останавливает текущий процесс индексации (переиндексации)
* `POST /api/indexPage` - добавляет в индекс или обновляет отдельную страницу,
адрес которой передан в параметре.
* `GET /api/statistics` - возвращает статистику и другую служебную
информацию о состоянии поисковых индексов и самого движка.
* `GET /api/search` - осуществляет поиск страниц по переданному
поисковому запросу (параметр query)

## Веб-интерфейс
Веб-интерфейс (frontend-составляющая) проекта представляет собой одну веб-страницу 
с тремя вкладками. С его помощью можно вызывать конечные точки API.

### Dashboard
Эта вкладка открывается по умолчанию. На ней отображается общая статистика 
по всем сайтам, а также детальная статистика и статус по каждому из сайтов 
(статистика, получаемая по запросу `/api/statistics`).

<img alt="Статистика" height="470" src="src\main\resources\images\Statistics.png"/>

### Management
На этой вкладке находятся инструменты управления поисковым движком — 
запуск и остановка полной индексации (переиндексации), а также возможность добавить 
(обновить) отдельную страницу по ссылке.

Кнопка `START INDEXING` запускает индексацию по списку сайтов из 
конфигурационного файла. При ее нажатии срабатывает конечная точка API 
`/api/startIndexing`.
После нажатия кнопка преобразуется в `STOP INDEXING`, которая останавливает 
индексацию, вызывая конечную точку API `/api/stopIndexing`.
> <u>ВАЖНО!</u> Индексация останавливается не сразу, необходимо некоторое время
> для полной остановки. Поэтому возможна ситуация, когда кнопку `STOP INDEXING`
> уже заменила кнопка `START INDEXING`, а индексация еще идет. В этом случае
> при нажатии на `START INDEXING` будет выдано предупреждение о том, что
> индексация уже запущена.
> 
> <img alt="Сообщение &quot;Индексация уже запущена&quot;" height="190" src="src\main\resources\images\IndexingAlreadyInProccess.png"/>

Кнопка `ADD/UPDATE` запускает переиндексацию отдельной страницы. Адрес страницы 
необходимо указать в текстовом поле левее кнопки. Если указанная страница
находится вне сайтов, указанных в конфигурационном файле, то будет выдано 
соответствующее сообщение и переиндексация не будет запущена.
При нажатии на кнопку `ADD/UPDATE` срабатывает конечная точка API `/api/indexPage`.

### Search
Эта страница предназначена для тестирования поискового движка. 
На ней находится поле поиска, выпадающий список с выбором сайта для поиска, 
а при нажатии на кнопку «Найти» выводятся результаты поиска 
(по API-запросу /api/search).

Результаты поиска выводятся в порядке убывания релевантности страницы 
поисковому запросу. Под каждой страницей представлен сниппет со словами 
из поискового запроса.

<img alt="Поисковый запрос" height="500" src="src\main\resources\images\GoodSearch.png"/>
<img height="240" src="src\main\resources\images\GoodSearchResponse.png"/>

Однако при поиске может и не быть результатов

<img alt="Поисковый запрос" height="200" src="src\main\resources\images\BadSearch.png"/>
<img height="100" src="src\main\resources\images\BadSearchResponse.png"/>

## Логирование
В проекте предусмотрена возможность логирования хода выполнения приложения.
Для реализации логирования используется библиотека Log4j2.

Ниже для примера приведены логи при получении статистики и начале полной 
индексации, когда в конфигурационном файле указаны два сайта:

<img alt="Пример логов" height="120" src="src\main\resources\images\StartFullIndexingLog.png"/>

Логи записываются в папку logs, которая автоматически создается в той 
директории, откуда было запущено приложение. 

Одновременно записывается общий файл лога `search-engine-log4j2-info.log` и 
лог, куда записываются только ошибки `search-engine-log4j2-error.log`.

Новый файл лога создается либо при новом запуске приложения,
либо при превышении размера в 100 MB. Старые файлы при этом переносятся в подпапку
с названием формата <текущий год>-<текущий месяц> и нумеруются.

Пример структуры директории логов представлен ниже:

<img height="200" src="src\main\resources\images\LogsDir.png"/>

Для изменения настроек логирования нужно отредактировать файл 
`src/main/resources/log4j2-spring.xml` и пересобрать проект.

## Инструкция по локальному запуску проекта

### Подготовка конфигурационного файла
* Скопировать из проекта файл `resources/application.yaml`.
* Указать верные данные для доступа к БД в разделе `spring.datasource` : логин и пароль,
а также адрес к используемой базы данных.
* В разделе `indexing-settings.sites` указать список сайтов, для которых предполагается проводить
индексацию и поиск.
* Если проект будет запускаться в консоли, которая не поддерживает ANSI-коды для выделения 
текста цветом, то для удобного чтения логов в консоли рекомендуется установить значение
настройки `output.ansi.enabled=never`.

### Подготовка jar файла
Собрать jar файл из исходников проекта можно с помощью maven 
командой package:
```shell
mvn package
```

### Запуск jar файла
В командной строке выполнить команду:
```shell
java -jar SearchEngine-1.5.0.jar --spring.config.location=application.yaml
```
Перейти в браузере по адресу http://localhost:8080

***
[![WorkStatus](https://img.shields.io/badge/Status-Complete-green.svg)](https://shields.io/)