# Pinkstack Realestate

Exploring ways of quick distributed scraping with the help of Akka.

## Standalone mode

The easiest way to run the scraper with stand-alone mode is to use the neat CLI interface.

```bash
# Build "fat jar" with SBT
sbt assembly

# Run it with 
java -jar target/*/scraper.jar --categories prodaja --pages 10
```

By default, the scraper spits out [JSON]. 

```bash
java -jar target/*/scraper.jar --categories prodaja --pages 2 | jq -R 'fromjson?'
```

So to make things bit easier for your eyes your 
can use [jq] to format or restructure output further for example to CSV.

```bash
java -jar target/*/scraper.jar --categories prodaja --pages 10 \
    | jq -R 'fromjson?' \
    | jq -r "([.refNumber, .title, .price, .location.latitude, .location.longitude]) | @csv" \
    > prodaja.csv
```

Adjusting parallelism and other [fine `application.conf` switches][configuration] can be easily done via loading of different configuration.

```bash
java -Dconfig.resource=quick.conf -jar target/*/scraper.jar --categories najem
```

Some [configuration options][configuration] can also be adjusted via environment variables i.e.

```bash
INITIAL_CATEGORIES=prodaja,najem
CATEGORY_PAGES_LIMIT=3
```

## Resources

- https://blog.softwaremill.com/running-akka-cluster-on-kubernetes-e4cd2913e951


## Authors

- [Oto Brglez](https://github.com/otobrglez)

[configuration]: src/main/resources/application.conf
[jq]: https://stedolan.github.io/jq/
[JSON]: https://www.json.org/json-en.html
