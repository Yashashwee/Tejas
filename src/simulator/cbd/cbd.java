

/*
  CBD or Constrained Based Debugging is a debugging feature
  that is intended to be a lightweight debugging solution.
  It achieves this by simply executing
  user defined constraints periodically. 

*/

package cbd;

import java.lang.reflect.*;
import java.lang.Class;
import java.util.Vector;

public class cbd {
    
    private static Vector<Method> methods;
    private static Vector<Vector<Object>> arguments;
    public static Object mutex;
    public  static long last_ran =0;

    public static void setup(String class_path,String method_name, Vector<Object> args) {

        //System.out.println("Setting up "+class_path+":"+method_name+"\n"+args);
        //String class_path = "memorysystem.Cache";
        //String method_name = "checkL1CachesHitRates";
        //Double parameter = new Double(0.85);
        int our_param_size = args.size();
        
        try {
            Class<?> c = Class.forName(class_path);
            //System.out.println("Found Class "+c);


            
            Method[] all_methods = c.getMethods();
            Method m = null;

            for( Method k: all_methods) {
                //System.out.print("Looking at "+k.getName()+" ..... ");
                
                if(!(k.getName().equals( method_name))) {
                    //System.out.print("FAILED 1\n");
                    continue;
                }

                Type[] types = k.getGenericParameterTypes();
                if(types.length != our_param_size) {
                    //System.out.print("FAILED 2 \n");
                    continue;
                }

                
                boolean types_good = true;
                for(int i=0;i<types.length;i++) {
                    if(!(types[i].getTypeName().equals( args.get(i).getClass().getTypeName()))) {
                        //System.out.println("Type mismatch for type "+(i+1)+". Expected "+ types[i].getTypeName() +". Got "+ args.get(i).getClass().getTypeName());
                        types_good = false;
                        break;
                    }
                }

                if(!types_good) {
                    //System.out.print("FAILED 3\n");
                    continue;
                }

                //Method Found
                m = k;
                break;
                
            }
            

            /*
              Class[] params_type = new Class[1];
              params_type[0] = parameter.getClass();
              System.out.println("Searching for "+params_type[0]);
              Method m = c.getMethod(method_name,params_type);
            */
            
            if( m!=null) {
                //System.out.println("Found Method "+m);
                methods.add(m);

                //Vector<Object> argvector = new Vector<Object>();
                //argvector.add(parameter);

                arguments.add(args);
            }
            else
                System.out.println("No matching method found at "+class_path+". Method searched was "+method_name+". Ignoring Constraint!!");
        } catch (Exception e) {
            e.printStackTrace();
        }

    }
    
    public static void init_cbd() {
        //init code
        methods = new Vector<Method>();
        arguments = new Vector<Vector<Object>>();
        mutex = new Object();

    }

    public static int getConstraintsSize() {
        
        return methods.size();
    }
    
    public static void run_constraints() {

        try{

            //calling code -----------
            for(int i=0;i<methods.size();i++) {
                Method mi         = methods  .get(i);
                Vector<Object> ai = arguments.get(i);

                Object[] array = ai.toArray(new Object[ai.size()]);

                
                Object ret = mi.invoke(null,array);

                if(ret instanceof Boolean) {
                    Boolean result = (Boolean) ret;

                    if(result.booleanValue()) {
                        System.out.println("Constaint " + mi.getName()+" ..... PASSED");
                    } else {
                        System.out.println("Constaint " + mi.getName()+" ..... FAILED");
                    }
                }
                else {
                    System.out.println("Constaint " + mi.getName()+" ..... did not return a boolean Value");
                }
                
                
            }
            //--------------------
                
        } catch(Exception e) {
            e.printStackTrace();
        }
        
    }
}
