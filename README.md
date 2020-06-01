# Comparing product prices with Akka

## Description
Akka app for comparing prices and finding product reviews. All actors and some part of system architecture are shown 
on the following diagram:

### Architecture diagram
![Whole system](doc/Akka_diagram.pdf)

Clients or web clients send ComparisonRequests (composing of product name) respectively to server or HTTP server. Then,
with help of some actors, all necessary data is fetched from and saved to SQLite database via DbClient. The request counter
is incremented with each price request from server. Responses might take a long time so there's timeout of 300ms defined.
If neither price is returned before time passes, the client receives appropriate response from server. The same pattern
applies to occurrence counter.

This app exposes following endpoints:
- /**price**/*product_name* - compares prices of specified product fetched from two "shops"
- /**review**/*product_name* - fetches advantages of a product from opineo.pl.

## How to run
Clone this repo and in your IDE run Main.java. Then, you can get price or review you want either by typing client number
and product name in terminal, or by going to localhost:8080/**appropriate_endpoint**/*product_name*.
Good luck and have fun! ;)