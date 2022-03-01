import 'dart:collection';
import 'dart:math';

import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:neat_periodic_task/neat_periodic_task.dart';
import 'package:intl/intl.dart';
import 'dart:convert';

void main() => runApp(const MyApp());


class MyApp extends StatelessWidget {
  const MyApp({Key? key}) : super(key: key);

  // This widget is the root of your application.
  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: 'Flutter Demo',
      theme: ThemeData(

        primarySwatch: Colors.blue,
      ),
      home: const MyHomePage(title: 'Flutter Demo Home Page'),
    );
  }
}

class MyHomePage extends StatefulWidget {
  const MyHomePage({Key? key, required this.title}) : super(key: key);

  final String title;

  @override
  State<MyHomePage> createState() => _MyHomePageState();
}

void printInfo(String text) {
  print('\x1B[33m$text\x1B[0m');
}

String getDebugDateFormat(final DateTime dt) {
  return DateFormat('hh:mm:ss a').format(dt);
}

class _MyHomePageState extends State<MyHomePage> {
  static const platform = MethodChannel('runbuddy/xmpp');
  NeatPeriodicTaskScheduler? pubTimer;
  NeatPeriodicTaskScheduler? jsonTimer;
  DateTime timeOfLastRecv = DateTime.now();
  DateTime timeOfLastJsonGenerate = DateTime.now();
  // Time interval to publish. Two minutes by default.
  int timeIntervalMs = 1000 * 60 * 2;
  int jsonTimerMS = 1000 * 60;
  Map<dynamic, dynamic> xmppItems = {};

  //
  List<Map<String, dynamic>> jsonQ = [{"time":DateTime.now().millisecond.toString()}];

  @override
  void initState() {
    getMessageFromXMPP().then((Map<dynamic, dynamic> message) {
      setState(() {
        xmppItems = message;
      });
    });

    pubTimer = createPubSubTimer();
    jsonTimer = createJsonTimer();
    super.initState();
  }

  NeatPeriodicTaskScheduler createJsonTimer() {
    if(jsonTimer != null) {
      jsonTimer!.stop();
      jsonTimer = null;
    }

    NeatPeriodicTaskScheduler newTimer = NeatPeriodicTaskScheduler(
      task: () async {
        printInfo("JSONing...");
        setState(() {
          jsonQ.add(json.decode('{'
          '"time": "${DateTime.now().millisecond.toString()}"'
              '}'));
          timeOfLastJsonGenerate = DateTime.now();
        });

        printInfo("Done JSONing.");
      },
      timeout: Duration(milliseconds: jsonTimerMS * 2),
      name: 'json-timer',
      minCycle: Duration(milliseconds: jsonTimerMS ~/ 2 - 1),
      interval: Duration(milliseconds: jsonTimerMS),
    );

    newTimer.start();
    return newTimer;
  }

  NeatPeriodicTaskScheduler createPubSubTimer() {
    // Stop the timer and null it so the garbage collector destroys it.
    if(pubTimer != null) {
      pubTimer!.stop();
      pubTimer = null;
    }

    NeatPeriodicTaskScheduler newTimer = NeatPeriodicTaskScheduler(
        task: () async {
          printInfo("Pubbing...");
          setState(() {
            getMessageFromXMPP().then((Map<dynamic, dynamic> message) {
              xmppItems = message;
            });
            timeOfLastRecv = DateTime.now();
          });

          printInfo("Done pubbing.");
        },
      timeout: Duration(milliseconds: timeIntervalMs * 2),
      name: 'pubsubber',
      minCycle: Duration(milliseconds: timeIntervalMs ~/ 2 - 1),
      interval: Duration(milliseconds: timeIntervalMs),
    );

    newTimer.start();
    return newTimer;
  }

  Future<Map<dynamic, dynamic>> getMessageFromXMPP() async {
    Map<dynamic, dynamic> value = {};

    try {
      List<String> times = [];
      for(var e in jsonQ) {
        if(e.containsKey('time')) {
          times.add('<data xmlns=\'https://example.org\'>App payload: ${e['time']}</data>' );
        }
      }
      jsonQ = [];
      value = await platform.invokeMethod('getMessage', times);
    } catch(e) {
      print(e);
    }

    return value;
  }

  // Build text object.
  List<Text> buildTextObj() {
    List<Text> result = [];

    // Add each map item.
    for(var e in xmppItems.entries) {
      result.add(Text("${e.key}: ${e.value}"));
    }

    result.add(const Text('----------Below are the objects-------------'));


    return List.from(result.reversed);
  }

  @override
  Widget build(BuildContext context) {
    var xmppList = buildTextObj();
    return Scaffold(
      appBar: AppBar(
        title: Text(widget.title),
      ),
      body: Center(
        child:
            Column(
              mainAxisAlignment: MainAxisAlignment.center,
              children: <Widget>[
                Text(
                  "Time of last recv is ${getDebugDateFormat(timeOfLastRecv)}",
                ),
                Text(
                  "Size of JSON queue is ${jsonQ.length}. Last JSON object added ${getDebugDateFormat(timeOfLastJsonGenerate)}."
                      "The Delay between JSON creations is $jsonTimerMS milliseconds.",
                ),
                Row(children: [
                  RaisedButton(onPressed: () { jsonTimerMS += 1000; jsonTimer = createJsonTimer(); },
                  child: const Icon(Icons.exposure_plus_1)),
                  RaisedButton(onPressed: () { jsonTimerMS = max(1000, timeIntervalMs - 1000); jsonTimer = createJsonTimer(); },
                  child: const Icon(Icons.exposure_neg_1)),
                ],),
                Expanded(
                    child: SizedBox(
                        height: 200.0,
                        child: ListView.builder(
                            itemCount: xmppItems.length,
                            itemBuilder: (context, index) {
                              var key = xmppItems.keys.elementAt(index);
                              return Card(child:
                              Text("$key: ${xmppItems[key]}")
                              );
                            }
                        )
                    )
                )
              ],
            ),
        )
      );
  }
}
