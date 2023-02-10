# Domain-side 

--- 

## Introduction

This is the center of the architecture. The other sides are driven by it, all the business logic goes here.

## About

The most important rule : **No dependencies** with any frameworks or big libraries. Business logic is as it says, the **logic** of your project's implementation. 
If I decide to switch to another framework, or more to change the database of the application, I can do it without touch any of this side of the application. That's the fundamental.


## Port and Adapters

We use the port / adapters design pattern to connect each side independently of the domain.
There is 2 kinds of ports : leftPorts for the client and rightPort for persistence. Other implementation mention that as Primary and Secondary ports. 

*Note that the left port contains only methods corresponding to the mentioned features of this application.*

Each side, left or right, has to implement at least one adapter that contains the methods to implements through your application.

The port's methods uses domain object as input and also as output, the adapters take care of mapping their own objects to manipulate them as they have to.