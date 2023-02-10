# Infrastructure 

This submodule contains all the technical supports used for the application. 
There is no business rules built in this module, only adapters drive or driven by the domain. 

## Implementation

This submodule is divided in 2 parts :
- Api 
- Spi

The Api is the left side of the hexagon, this is the endpoint between client and the domain and its adapters contains all the concrete features.

The Spi is the right side that allow persistence from/retrieving to the database. 