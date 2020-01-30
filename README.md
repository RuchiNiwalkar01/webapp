# CSYE 6225 - Spring 2020
---

## Technology Stack
---

Programming Language and framework used: Java, Spring Boot Framework, MySQL

Prerequisites for building the application:

* Install Spring Tool Suite (STS)
* Install Postman
* Install MySQL

## Build Instructions
---

1.  Import the application from webapp/project folder into STS
2.  Configure the application.properties by adding your database connection
3.  Run the application as 'Spring Boot App'
4.  To test the API results, go to Postman application.
5.  Now select the POST option and enter the URL as "http://localhost:8080/v1/user" with key Content-Type
6.  In the body section below, select 'raw' and then select 'JSON(application/json)'
7.  Write the parameters to be sent in JSON format and click on 'Send', see the results on the window below.
8.  If the username already exists or password length does not match, required status code/message is shown.
9.  Now select GET option and enter the URL as "http://localhost:8080/v1/user/self"
10. In the 'authorization' section, select 'Basic Auth'
11. Enter the credentials provided in step 7 and click 'Send'.
12. If the credentials are correct, the given response is shown with correct status codes.
13. Now select PUT option and enter the URL as "http://localhost:8080/v1/user/self"
14. If the credentials are correct, the user is updated correct status codes.
15. Make POST http://localhost:8080/v1/bill request for creating a bill with required parameters.
16. Make PUT http://localhost:8080/v1/bill/id request for updating a bill with required parameters.
17. Make GET http://localhost:8080/v1/bills request for fetching all the bill for a user.
18. Make GET http://localhost:8080/v1/bill/id request for fetching a particular bill for that user.
19. Make DELETE http://localhost:8080/v1/bill/id request for deleting a bill.

## Running Tests
---

Run the unit test from test package- "Run as JUnit"

## CI/CD
---

Make commit to the git repository to invoke the build in CircleCI for all the repositories
