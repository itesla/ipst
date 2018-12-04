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

# simple dump network flows function
def dump_lines_flow(network):
    print(len(network.get_lines()))
    for l in network.get_lines():
        print(l.get_id() + ";" + str(l.get_terminal_1().get_i()) + ";" + str(l.get_terminal_2().get_i()))
        print(l.get_current_limits_1().get_permanent_limit())
        print(str(l.check_permanent_limit_1()) + ";" + str(l.check_permanent_limit_2()))
        print(str(l.check_permanent_limit_1(0.1)) + ";" + str(l.check_permanent_limit_2(0.1)))
        print(str(l.is_overloaded()) + ";" + str(l.is_overloaded(0.1)))

if __name__ == '__main__':
    port = 3338
    launch("config2", port)
    if connect(port):
        n1 = load("/path/to/case-file/example.xiidm")
        dump_lines_flow(n1)

        lf = run_load_flow(n1)
        print("\nLF result: " + str(lf.is_ok()) + "; metrics: " + str(lf.get_metrics()))
        dump_lines_flow(n1)

        # re-run load flow alternatively
        lf = n1.run_load_flow()
        print("\nLF result: " + str(lf.is_ok()) + "; metrics: " + str(lf.get_metrics()))
        
        n1.save("/path/to/output/example.xiidm")
        # or save(n1, "/path/to/output/example.xiidm")

        # don't forget to shundown jvm
        shundown_pypowsybl()
    else:
        print("can not connect to jvm")

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
