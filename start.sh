export CLASSPATH=./classes;./src

for jar in ./libs/*.jar;do

 export CLASSPATH=$CLASSPATH:$jar
 
done

for jar in ./libs/rome/*.jar;do

export CLASSPATH=$CLASSPATH:$jar
 
done

java -classpath $CLASSPATH io.kurumi.ntt.Launcher