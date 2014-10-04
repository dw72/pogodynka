package pl.gostyn.mojapogodynka;

import android.app.ActionBar;
import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.ActionBarDrawerToggle;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.Gson;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.TimeZone;

import pl.gostyn.mojapogodynka.model.City;
import pl.gostyn.mojapogodynka.model.Weather;


public class MainActivity extends Activity {
    private static final String PREFS_FILE = "WeatherPrefsFile";
    private static final String PREFS_DRAWER_POSITION = "drawer_position";

    private static final String WEATHER_URL = "http://api.openweathermap.org/data/2.5/weather?APPID=232164e69796bda78852551b1fa13a36&lang=pl&type=like&units=metric&id=";

    private DrawerLayout mDrawerLayout;
    private ListView mDrawerList;
    private ArrayAdapter<City> mDrawerListAdapter;
    private ActionBarDrawerToggle mDrawerToggle;

    private CharSequence mDrawerTitle;
    private CharSequence mTitle;

    private final ArrayList<City> mCities = new ArrayList<City>();

    private int mSelectedPosition;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mTitle = mDrawerTitle = getTitle();

        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        mDrawerList = (ListView) findViewById(R.id.left_drawer);

        mDrawerLayout.setDrawerShadow(R.drawable.drawer_shadow, GravityCompat.START);
        mDrawerListAdapter = new ArrayAdapter<City>(this, R.layout.drawer_list_item, mCities);
        mDrawerList.setAdapter(mDrawerListAdapter);
        mDrawerList.setOnItemClickListener(new DrawerItemClickListener());

        // TODO: Wczytywanie zapisanych miejscowości
        mDrawerListAdapter.add(new City("Gostyń", 3098625));
        mDrawerListAdapter.add(new City("Krotoszyn", 3094625));
        mDrawerListAdapter.add(new City("Leszno", 3093524));
        mDrawerListAdapter.add(new City("Suwałki", 7530941));

        loadSettings();

        final ActionBar ab = getActionBar();
        if (ab != null) {
            ab.setDisplayHomeAsUpEnabled(true);
            ab.setHomeButtonEnabled(true);

            mDrawerToggle = new ActionBarDrawerToggle(
                    this,
                    mDrawerLayout,
                    R.drawable.ic_drawer,
                    R.string.navigation_drawer_open,
                    R.string.navigation_drawer_close
            ) {
                public void onDrawerClosed() {
                    ab.setTitle(mTitle);
                    invalidateOptionsMenu();
                }

                public void onDrawerOpened() {
                    ab.setTitle(mDrawerTitle);
                    invalidateOptionsMenu();
                }
            };
            mDrawerLayout.setDrawerListener(mDrawerToggle);
        }
        if (savedInstanceState == null) {
            selectItem(mSelectedPosition);
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        saveSettings();
    }

    private void loadSettings() {
        SharedPreferences settings = getSharedPreferences(PREFS_FILE, 0);
        mSelectedPosition = settings.getInt(PREFS_DRAWER_POSITION, 0);
    }

    private void saveSettings() {
        SharedPreferences settings = getSharedPreferences(PREFS_FILE, 0);
        SharedPreferences.Editor editor = settings.edit();
        editor.putInt(PREFS_DRAWER_POSITION, mSelectedPosition);
        editor.apply();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater i = getMenuInflater();
        i.inflate(R.menu.main, menu);

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        boolean drawerOpen = mDrawerLayout.isDrawerOpen(mDrawerList);
        menu.findItem(R.id.action_refresh).setVisible(!drawerOpen);

        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (mDrawerToggle.onOptionsItemSelected(item)) {
            return true;
        }

        switch (item.getItemId()) {
            case R.id.action_add:
                // TODO: GUI dodawania nowej miejscowości
                mDrawerListAdapter.add(new City("Warszawa", 756135));
                mDrawerListAdapter.add(new City("New York", 5128581));
                mDrawerListAdapter.add(new City("Kiruna", 605155));
                mDrawerListAdapter.add(new City("Delhi", 1273294));

                selectItem(mDrawerList.getCount() - 1);
                return true;

            case R.id.action_refresh:
                selectItem(mSelectedPosition);
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void selectItem(int position) {
        if (!mCities.isEmpty()) {
            if (position >= mCities.size()) {
                position = 0;
            }

            int cityId = mCities.get(position).id;

            Fragment f = new WeatherFragment();
            Bundle args = new Bundle();
            args.putInt(WeatherFragment.ARG_CARD_NUMBER, position);
            args.putInt(WeatherFragment.ARG_WEATHER_ID, cityId);
            f.setArguments(args);

            FragmentManager fm = getFragmentManager();
            fm.beginTransaction().replace(R.id.container, f).commit();

            if (isNetworkConnected()) {
                new DownloadWeatherTask().execute(WEATHER_URL + cityId);
            }

            mDrawerList.setItemChecked(position, true);
            setTitle(mCities.get(position).name);
            mDrawerLayout.closeDrawer(mDrawerList);

            mSelectedPosition = position;
        }
    }

    private String downloadWeather(String url) throws IOException {
        InputStream is = null;
        try {
            HttpURLConnection c = getHttpURLConnection(url);
            is = c.getInputStream();

            return getStringFromInputStream(is);

        } finally {
            if (is != null) {
                is.close();
            }
        }
    }

    private boolean isNetworkConnected() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = cm.getActiveNetworkInfo();

        if (networkInfo != null && networkInfo.isConnected()) {
            return true;

        } else {
            String msg = "Network is not connected";
            Toast.makeText(getBaseContext(), msg, Toast.LENGTH_SHORT).show();
            return false;
        }
    }

    private HttpURLConnection getHttpURLConnection(String url) throws IOException {
        HttpURLConnection c = (HttpURLConnection) (new URL(url)).openConnection();
        c.setReadTimeout(10000); // milisekundy
        c.setConnectTimeout(15000); // milisekundy
        c.setRequestMethod("GET");
        c.setDoInput(true);
        c.connect();
        return c;
    }

    private String getStringFromInputStream(InputStream is) throws IOException {
        BufferedReader streamReader = new BufferedReader(new InputStreamReader(is));
        StringBuilder stringBuilder = new StringBuilder();

        String inputString;
        while ((inputString = streamReader.readLine()) != null) {
            stringBuilder.append(inputString);
        }

        return stringBuilder.toString();
    }

    @Override
    public void setTitle(CharSequence title) {
        mTitle = title;
        ActionBar ab = getActionBar();
        if (ab != null) {
            ab.setTitle(mTitle);
        }
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        mDrawerToggle.syncState();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        mDrawerToggle.onConfigurationChanged(newConfig);
    }

    private class DrawerItemClickListener implements android.widget.AdapterView.OnItemClickListener {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            selectItem(position);
        }
    }

    private static Date getLocalTime(long unixTimeStamp) {
        Calendar cal = GregorianCalendar.getInstance();
        TimeZone from = TimeZone.getTimeZone("UTC");
        TimeZone to = TimeZone.getDefault(); // W emulatorze zwraca UTC zamiast lokalnej strefy

        cal.setTimeZone(from);
        cal.setTimeInMillis(unixTimeStamp * 1000L);
        cal.setTimeZone(to);

        return cal.getTime();
    }

    private void setWeatherData(Weather weather) {
        ((TextView) findViewById(R.id.condDescr)).setText(weather.conditions[0].description);
        ((TextView) findViewById(R.id.temp)).setText(String.format("%.0f \u2103", weather.parameters.temp));
        ((TextView) findViewById(R.id.press)).setText(String.format("%.2f hPa", weather.parameters.pressure));
        ((TextView) findViewById(R.id.hum)).setText(String.format("%d %%", weather.parameters.humidity));
        ((TextView) findViewById(R.id.windSpeed)).setText(String.format("%.2f m/s", weather.wind.speed));
        ((TextView) findViewById(R.id.windDeg)).setText(String.format("(%.4f \u00B0)", weather.wind.deg));

        ((TextView) findViewById(R.id.sunrise)).setText(String.format("%tR", getLocalTime(weather.sun.sunrise)));
        ((TextView) findViewById(R.id.sunset)).setText(String.format("%tR", getLocalTime(weather.sun.sunset)));

        String imgName = String.format("weather_%s", weather.conditions[0].icon);
        int imgId = getResources().getIdentifier(imgName, "drawable", this.getPackageName());
        ((ImageView) findViewById(R.id.condIcon)).setImageResource(imgId);
    }

    private class DownloadWeatherTask extends AsyncTask<String, Void, String> {

        @Override
        protected String doInBackground(String... urls) {
            try {
                return downloadWeather(urls[0]);
            } catch (IOException e) {
                String msg = "Unable to retrieve weather data";
                Toast.makeText(getBaseContext(), msg, Toast.LENGTH_SHORT).show();
                return msg;
            }
        }

        @Override
        protected void onPostExecute(String string) {
            Weather weather = (new Gson()).fromJson(string, Weather.class);

            if (weather.httpResponseCode == 200) {
                setWeatherData(weather);
            }
            else {
                String msg = String.format(getString(R.string.bad_server_response), weather.httpResponseCode);
                Toast.makeText(getBaseContext(), msg, Toast.LENGTH_SHORT).show();
            }
        }
    }

    public static class WeatherFragment extends Fragment {
        public static final String ARG_CARD_NUMBER = "card_number";
        public static final String ARG_WEATHER_ID = "weather_id";


        public WeatherFragment() {
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
            return inflater.inflate(R.layout.fragment_weather, container, false);
        }
    }
}
