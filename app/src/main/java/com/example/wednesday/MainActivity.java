package com.example.wednesday;


import java.io.BufferedWriter;
import java.io.OutputStreamWriter;
import android.os.Bundle;
import android.os.StrictMode;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;

import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {
    private EditText taskEditText, dateEditText;
    private Button addButton, updateButton, deleteButton;
    private ListView taskListView;
    private ArrayAdapter<String> taskAdapter;
    private ArrayList<String> taskList;
    private int selectedTaskIndex = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);

        taskEditText = findViewById(R.id.taskEditText);
        dateEditText = findViewById(R.id.dateEditText);
        addButton = findViewById(R.id.addButton);
        updateButton = findViewById(R.id.updateButton);
        deleteButton = findViewById(R.id.deleteButton);
        taskListView = findViewById(R.id.taskListView);
        taskList = new ArrayList<>();
        taskAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, taskList);
        taskListView.setAdapter(taskAdapter);

        taskListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                selectedTaskIndex = position;
                String selectedItem = taskList.get(position);
                String[] parts = selectedItem.split(" \\(Date: ");
                String task = parts[0];
                String date = parts[1].substring(0, parts[1].length() - 1);
                taskEditText.setText(task);
                dateEditText.setText(date);
                updateButton.setVisibility(View.VISIBLE);
                deleteButton.setVisibility(View.VISIBLE);
            }
        });

        addButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                addTask();
            }
        });

        updateButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                updateTask();
            }
        });

        deleteButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                deleteTask();
            }
        });

        loadTasks();
    }

    private void addTask() {
        final String task = taskEditText.getText().toString();
        final String date = dateEditText.getText().toString();
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    URL url = new URL("http://192.168.1.144:5000/tasks");
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    conn.setRequestMethod("POST");
                    conn.setRequestProperty("Content-Type", "application/json;charset=UTF-8");
                    conn.setRequestProperty("Accept", "application/json");
                    conn.setDoOutput(true);
                    conn.setDoInput(true);
                    JSONObject jsonParam = new JSONObject();
                    jsonParam.put("task", task);
                    jsonParam.put("date", date);
                    OutputStream os = conn.getOutputStream();
                    os.write(jsonParam.toString().getBytes("UTF-8"));
                    os.flush();
                    os.close();
                    int responseCode = conn.getResponseCode();
                    if (responseCode == 200) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                taskEditText.setText("");
                                dateEditText.setText("");
                                loadTasks();
                                showToast("Görev başarıyla eklendi.");
                            }
                        });
                    } else {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                showToast("Görev eklenirken hata oluştu. API yanıt kodu: " + responseCode);
                            }
                        });
                    }
                    conn.disconnect();
                } catch (Exception e) {
                    e.printStackTrace();
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            showToast("Hata: " + e.getMessage());
                        }
                    });
                }
            }
        }).start();
    }

    private void loadTasks() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    URL url = new URL("http://192.168.1.144:5000/tasks");
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    conn.setRequestMethod("GET");
                    int responseCode = conn.getResponseCode();
                    if (responseCode == 200) {
                        BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                        StringBuilder response = new StringBuilder();
                        String line;
                        while ((line = br.readLine()) != null) {
                            response.append(line);
                        }
                        br.close();
                        final JSONArray jsonArray = new JSONArray(response.toString());
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                taskList.clear();
                                for (int i = 0; i < jsonArray.length(); i++) {
                                    try {
                                        JSONObject jsonObject = jsonArray.getJSONObject(i);
                                        String taskId = jsonObject.getString("id");
                                        String task = jsonObject.getString("task");
                                        String date = jsonObject.getString("date");
                                        taskList.add(task + " (Date: " + date + ")");
                                    } catch (JSONException e) {
                                        e.printStackTrace();
                                    }
                                }
                                taskAdapter.notifyDataSetChanged();
                            }
                        });
                    } else {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                showToast("Veriler alınırken hata oluştu.");
                            }
                        });
                    }
                    conn.disconnect();
                } catch (Exception e) {
                    e.printStackTrace();
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            showToast("Hata: " + e.getMessage());
                        }
                    });
                }
            }
        }).start();
    }

    private void deleteTask() {
        if (selectedTaskIndex != -1) {
            String selectedItem = taskList.get(selectedTaskIndex);
            String[] parts = selectedItem.split(" \\(Date: ");
            final String taskToDelete = parts[0];
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {

                        URL url = new URL("http://192.168.1.144:5000/tasks/" + taskToDelete);
                        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                        conn.setRequestMethod("DELETE");
                        int responseCode = conn.getResponseCode();
                        conn.disconnect();

                        if (responseCode == 200) {
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    taskEditText.setText("");
                                    dateEditText.setText("");
                                    selectedTaskIndex = -1;
                                    updateButton.setVisibility(View.GONE);
                                    deleteButton.setVisibility(View.GONE);
                                    loadTasks();
                                    showToast("Görev başarıyla silindi.");
                                }
                            });
                        } else if (responseCode == 404) {
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    showToast("Silinecek görev bulunamadı.");
                                }
                            });
                        } else {
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    showToast("Görev silinirken hata oluştu. API yanıt kodu: " + responseCode);
                                }
                            });
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                showToast("Hata: " + e.getMessage());
                            }
                        });
                    }
                }
            }).start();
        } else {
            showToast("Silinecek bir görev seçiniz.");
        }
    }

    private void updateTask() {
        if (selectedTaskIndex != -1) {
            final String newTask = taskEditText.getText().toString();
            final String newDate = dateEditText.getText().toString();
            String selectedItem = taskList.get(selectedTaskIndex);
            String[] parts = selectedItem.split(" \\(Date: ");
            final String oldTask = parts[0];


            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        URL url = new URL("http://192.168.1.144:5000/tasks/" + oldTask);
                        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                        conn.setRequestMethod("PUT");
                        conn.setRequestProperty("Content-Type", "application/json;charset=UTF-8");
                        conn.setRequestProperty("Accept", "application/json");
                        conn.setDoOutput(true);
                        conn.setDoInput(true);


                        JSONObject jsonParam = new JSONObject();
                        jsonParam.put("task", newTask);
                        jsonParam.put("date", newDate);

                        OutputStream os = conn.getOutputStream();
                        BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(os, "UTF-8"));
                        writer.write(jsonParam.toString());
                        writer.flush();
                        writer.close();
                        os.close();

                        int responseCode = conn.getResponseCode();
                        if (responseCode == 200) {
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    taskEditText.setText("");
                                    dateEditText.setText("");
                                    selectedTaskIndex = -1;
                                    updateButton.setVisibility(View.GONE);
                                    deleteButton.setVisibility(View.GONE);
                                    loadTasks();
                                    showToast("Görev başarıyla güncellendi.");
                                }
                            });
                        } else {
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    showToast("Görev güncellenirken hata oluştu. API yanıt kodu: " + responseCode);
                                }
                            });
                        }
                        conn.disconnect();
                    } catch (Exception e) {
                        e.printStackTrace();
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                showToast("Hata: " + e.getMessage());
                            }
                        });
                    }
                }
            }).start();
        } else {
            showToast("Güncellenecek bir görev seçiniz.");
        }
    }

    private void showToast(final String message) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT).show();
            }
        });
    }


}
