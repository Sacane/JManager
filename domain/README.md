# Domain-side 

--- 

## Introduction

This is the center of the architecture. The other sides are driven by it, all the business logic goes here.

## What's important

The most important rule : **No dependencies** with any frameworks or big libraries. Business logic is as it says, the **logic** of your project's implementation. 
If I decide to switch to another framework, or more to change the database of the application, I can do it without touch any of this side of the application. That's the fundamental.

## My models

There is 3 mains entities in this project : 

* **User** : the users that can access to the application
* **Account** : containers of user's budget
* **Sheet** : All sheets that resume each user's expenses or incomes



## Encapsulation

Some of my classes are only encapsulation for primitives. For example : UserId for a Long value of the user's id, or a password which are the user's password as string.

Two main reasons for this implementation : 
* To be close with the DDD concepts, an outsider can read this part of a code. When he looks this project he can easily check that a user has a Password as attribute, and not a String which represent a password. This part is one of the reason this architecture exists. 
* To encapsulate the business logic inside the encapsulated object. If I want passwords in this application to have a certain pattern, I have to specify it in the domain.

## Port and Adapters

We use the port / adapters design pattern to connect each side independently of the domain. Actually the other side could (Or maybe should..) be implemented as other distinct projects, but in my implementation I decided not to do so. And the communication is carried out thanks to the interface port / adapters. 


Each side, server or user, has to implement at least one adapter that contains the methods to implements through your application : 
* Queries, search for an object
* Actions like persistence of an object, or business ones

The port's methods uses domain object as entry, the adapters take care of mapping their own objects to manipulate them as they have to.


## Notes

*For now this application looks like a crud application, but a lots of business logic are coming in future features that will bring the value of this architecture*