# JManager's back-end (Architecture hexagonal implementation)

## Introduction

--- 

JManager is a WebApp that allow you to manage your personal budget. And this project is the Back-end side of the project.


## What's behind this application ?

--- 

This project is my first attempt to build a **business-based application**. More precisely, an app centered by the customer business expectation.
I also wanted it to be as flexible as possible. 
To do this, I tried to implement the concept of **Hexagonal architecture**, at least my version of this architecture.

Here you can find a good topic about this architecture [In french](https://blog.octo.com/architecture-hexagonale-trois-principes-et-un-exemple-dimplementation/) or another topic in [english](https://medium.com/ssense-tech/hexagonal-architecture-there-are-always-two-sides-to-every-story-bc0780ed7d9c).

## So what's my implementation of this architecture ? 

--- 

This architecture does separate the project in 3 distinct subprojects :
* **Domain-side** which contains all the business _rules_
* **Server-side** which contains all the database interaction, or files system calls. Shortly, the persistence side. 
* **User-side**, or the side which interact directly with the user. It could be an API (which is actually the case), or a command-line interface.

## Why this architecture ?

--- 

Because I felt in love with this architecture. 

Seriously, this is the main reason I implemented this architecture in this project : 

* To be as flexible as possible by implementing each side separately
* To feel free to change my technos at any time for future releases without break all of my application. 
* To test each side independently, or only centered on the domain-side. 
* To make a first step with DDD (Domain driven design)
* Because I like the port/adapter design pattern. 
* Because I had time to do this

### Technos : 

---

* Kotlin
* Framework Spring for user and server-side implementation (Ktor is coming soon)
* _PostgreSQL_ for persistence

