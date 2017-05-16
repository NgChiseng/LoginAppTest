package com.idbc.cngyu.loginapptest;

import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;

public class MainActivity extends AppCompatActivity {

    // All global variables are declared
    EditText password_field, phone_field;
    TextView recover_password;
    Button log_in;
    ProgressBar login_progress_bar;

    /*
        Function that define the event associated to the create state and render the main screen
        view with the .xml linked.
        @date [14/05/2017]
        @author [ChiSeng Ng]
        @param [View] view Object view associated to the button on clicked
        @return [Void] None
    */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // This will link layout .xml with the actual activity  .java
        setContentView(R.layout.activity_main);

        //  Example of accessing resource in Code
        TextView title_login = (TextView) findViewById(R.id.title);
        title_login.setText(R.string.login);

        // This will instance the variables declared with the views elements of the layout
        phone_field = (EditText) findViewById(R.id.phone_field);
        password_field = (EditText) findViewById(R.id.password_field);

        // This will instance the button variable and define result when click its
        log_in = (Button) findViewById(R.id.Login_button);
        log_in.setOnClickListener(new OnClickListener() {

            /*
                Function that define the event will occur according to the entry that user inserted
                @date [14/05/2017]
                @author [ChiSeng Ng]
                @param [View] view Object view associated to the button on clicked
                @return [Void] None
            */
            @Override
            public void onClick(View view) {

                //String that define the valid format for a phone_number
                final String valid_number_format = "^0(2|4)\\d{9}$";

                //This take the phone number String for evaluate its format
                String phone_field_str = phone_field.getText().toString();

                //This evaluate if phoneField have a valid format
                if (phone_field_str.matches(valid_number_format)) {
                    AttemptLogIn attempt = new AttemptLogIn();
                    attempt.execute(phone_field_str, password_field.getText().toString());
                } else {
                    Toast.makeText(getBaseContext(), R.string.invalid_format, Toast.LENGTH_SHORT).show();
                }
            }
        });

        login_progress_bar = (ProgressBar) findViewById(R.id.login_progress_bar);
    }

    /*
        Class that implemented the attempt of the connect with the server, insert and valid the login
    data, through an AsyncTask
    */
    private class AttemptLogIn extends AsyncTask<String,Integer,Integer>{

        /*
            Function that show the progress bar like previous work before to execute the AsyncTask
            @date [15/05/2017]
            @author [ChiSeng Ng]
            @param [None] None
            @return [Void] None
        */
        @Override
        protected void onPreExecute(){

            //This make visible the progress bar of the login in the main screen.
            login_progress_bar.setVisibility(View.VISIBLE);
        }

        // Sends validated Log In's data to the server's API and process the response. Returns an
        // integer value ([-1..1):
        // * -1, if an error occurred during the communication
        // * 0, if everything went OK (redirecting to MainActivity and updating SharedPreferences afterwards)
        // * 1, if the credentials provided aren't valid
        @Override
        protected Integer doInBackground(String... strings) {

            Integer result = -1;
            try {
                // Defining and initializing server's communication's variables
                String credentials = URLEncoder.encode("username", "UTF-8") + "=" + URLEncoder.encode(strings[0], "UTF-8");
                credentials += "&" + URLEncoder.encode("password", "UTF-8") + "=" + URLEncoder.encode(strings[1], "UTF-8");

                URL url = new URL("http://agruppastage.herokuapp.com/api-token-auth/");
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setDoOutput(true);
                connection.setRequestMethod("POST");
                connection.setReadTimeout(10000);
                OutputStreamWriter writer = new OutputStreamWriter(connection.getOutputStream());
                writer.write(credentials);
                writer.flush();
                APIResponse response = JSONResponseController.getJsonResponse(connection);

                Log.w("AUXILIO", String.valueOf(response.getStatus()));

                if (response != null) {
                    if (response.getStatus() == HttpURLConnection.HTTP_OK) {
                        JSONObject jsonResponse = response.getBody();
                        String token = jsonResponse.getString("token");
                        Integer id = jsonResponse.getInt("id");
                        String role = jsonResponse.getString("role");
                        TokenSharedPreferences.setAuthToken(LoginActivity.this, token);
                        UserPKSharedPreferences.setUserPK(LoginActivity.this, id);
                        UserRoleSharedPreferences.setUserRole(LoginActivity.this, role);
                        result = 0;

                    } else if (response.getStatus() == HttpURLConnection.HTTP_BAD_REQUEST) {
                        JSONObject jsonResponse = response.getBody();
                        String responseMessage = jsonResponse.getJSONArray("non_field_errors").getString(0);
                        if (responseMessage.equals("Unable to log in with provided credentials.")) {
                            result = 1;
                        }
                    } else if (response.getStatus() == HttpURLConnection.HTTP_NOT_FOUND) {
                        JSONObject jsonResponse = response.getBody();
                        String responseMessage = jsonResponse.getString("detail");
                        Log.w("AUXILIO", responseMessage);
                        if (responseMessage.equals("Not found.")) {
                            result = -1;
                        }
                    }
                }

            } catch (MalformedURLException e) {
                return result;
            } catch (IOException e) {
                return result;
            } catch (JSONException e) {
                return result;
            }
            return result;
        }

        // Process doInBackground() results
        @Override
        protected void onPostExecute(Integer anInt) {
            String message;
            switch (anInt) {
                case (-1):
                    message = "Ha habido un problema conectando con el servidor, intente de nuevo más tarde";
                    Toast.makeText(getBaseContext(), message, Toast.LENGTH_SHORT).show();
                    progressBar.setVisibility(View.GONE);
                    break;
                case (0):
                    Intent intent = new Intent(getBaseContext(), DashboardActivity.class);
                    message = "¡Bienvenido!";
                    Toast.makeText(getBaseContext(), message, Toast.LENGTH_SHORT).show();
                    startActivity(intent);
                    progressBar.setVisibility(View.GONE);
                    break;
                case (1):
                    message = "Nombre de usuario y/o contraseña inválidos";
                    Toast.makeText(getBaseContext(), message, Toast.LENGTH_SHORT).show();
                    progressBar.setVisibility(View.GONE);
                    break;
                default:
                    break;
            }
        }
    }
}

