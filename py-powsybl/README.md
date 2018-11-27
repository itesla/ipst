# Py-powsybl:  how-to use the powsybl APIs from Python

py-powsybl module uses [py4j](https://www.py4j.org) to call the Powsybl java APIs, from a Python environment.
 
### Runtime requirements:
Py4j enables Python programs running in a Python interpreter to dynamically access Java objects in a Java Virtual Machine.

Py4j documentation states that it has been tested with Python 2.6, 2.7, 3.4, 3.5, and 3.6
To install Py4j v0.10.7 on CentOS (detailed information [here](https://www.py4j.org/install.html#id1):
    
    pip install py4j (Python v2)
    
or
    
    pip3 install py4j (Python v3)
 
Note: when it comes to experimenting with multiple Python extensions, it might be a good idea to create first an isolated python environment with [virtualenv](https://virtualenv.pypa.io/en/stable/)
       
### Execute py-powsybl

Start the py-powsybl 'server' in a console. 

    itools py-powsybl
   
### Run some python code:

Demo1.py demostrates what can be done (it executes a loadflow on a network, opens a switch, exports a network to a file in xiidm format)

    python Demo1.py     
    
Another example using pypowsybl module
    
```
from pypowsybl import *

load("/path/to/case-file/example.xiidm")

lf = run_load_flow()
print("\nLF result: " + str(lf.isOk()) + "; metrics: " + str(lf.getMetrics()))

network = get_network()
# modify network

# re-run load flow
lf = run_load_flow()
print("\nLF result: " + str(lf.isOk()) + "; metrics: " + str(lf.getMetrics()))
save("/path/to/output/example.xiidm")

# shundown jvm
shundown_pypowsybl()
```

### Stop py-powsybl
To stop the py-powsybl 'server',  CTRL+C in the itools console. 
 

## Notes
py-powsybl has been tested on a CentOS 6, with python v2.6, python v3.6 (in a virtualenv sandbox).

## TODO
* performances/memory with multiple, large networks/data ? 
* make the client/server connection configurable (currently, it uses the defaults py4j)
* explore the other way round (calling python from java)
* issues? threads? security? 
