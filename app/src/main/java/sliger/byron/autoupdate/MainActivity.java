package sliger.byron.autoupdate;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;

public class MainActivity extends AppCompatActivity {

    public static final int UNINSTALL_CODE = 1;
    public static final int INSTALL_CODE = 2;
    private Autoupdater updater;
    private Context context;
    private Button actualizarBTN;
    private TextView textViewActualizaciones;
    private Activity activity;
    private ProgressDialog loadingPanel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        activity = this;
        showProgressBar();
        loadingPanel.hide();
        try {
            actualizarBTN = (Button) findViewById(R.id.botonActualizar);
            textViewActualizaciones = (TextView) findViewById(R.id.textViewActualizaciones);
            actualizarBTN.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    comenzarActualizacion();
                }
            });
        }catch (Exception ex){
            //Por Cualquier error.
            Toast.makeText(this,ex.getMessage(),Toast.LENGTH_LONG);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode){
            case UNINSTALL_CODE:
                if(resultCode == RESULT_OK){
                    textViewActualizaciones.setText("La version " + updater.getCurrentVersionName() + " fue eliminada.");
                    textViewActualizaciones.setTextColor(Color.BLUE);
                    loadingPanel.setMessage("Descargando Version " + updater.getLatestVersionName() + "...");
                    updater.InstallNewVersion(ocultarLoadingPanel);
                } else {
                    textViewActualizaciones.setText("Fue cancelada la acción, es necesario eliminar la version " + updater.getCurrentVersionName() +".");
                    textViewActualizaciones.setTextColor(Color.RED);
                    loadingPanel.hide();
                }
                break;
            case INSTALL_CODE:
                if(resultCode == RESULT_OK){
                    loadingPanel.hide();
                    textViewActualizaciones.setText("La version " + updater.getLatestVersionName() + " fue instalada correctamente.");
                    textViewActualizaciones.setTextColor(Color.BLUE);
                } else {
                    textViewActualizaciones.setText("Fue cancelada la acción, no fue instalada la version " + updater.getLatestVersionName() +".");
                    textViewActualizaciones.setTextColor(Color.RED);
                    loadingPanel.hide();
                }
                break;
        }
    }

    private void showProgressBar (){
        loadingPanel=new ProgressDialog(this);
        loadingPanel.setMessage("Procesanado...");
        loadingPanel.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        loadingPanel.setIndeterminate(true);

    }

    private void comenzarActualizacion(){
        //Para tener el contexto mas a mano.
        context = this;
        //Creamos el Autoupdater.
        updater = new Autoupdater(this);
        //Ponemos a correr el ProgressBar.
        loadingPanel.setMessage("Consultando Informacion Version...");
        loadingPanel.show();
        //Ejecutamos el primer metodo del Autoupdater.
        updater.DownloadData(finishBackgroundDownload);
    }

    /**
     * Codigo que se va a ejecutar una vez terminado de bajar los datos.
     */
    private Runnable finishBackgroundDownload = new Runnable() {
        @Override
        public void run() {
            //Volvemos el ProgressBar a invisible.
            loadingPanel.hide();
            //Comprueba que halla nueva versión.
            if(updater.isNewVersionAvailable()){
                //Crea mensaje con datos de versión.
                String msj = "Nueva Versión: ";
                msj += "\nVersió Actual : " + updater.getCurrentVersionName() + "(" + updater.getCurrentVersionCode() + ")";
                msj += "\nUltima Versión: " + updater.getLatestVersionName() + "(" + updater.getLatestVersionCode() +")";
                msj += "\n¿Desea Actualizar?";
                //Crea ventana de alerta.
                AlertDialog.Builder dialog1 = new AlertDialog.Builder(context);
                dialog1.setMessage(msj);
                dialog1.setNegativeButton(R.string.cancel, null);
                //Establece el boton de Aceptar y que hacer si se selecciona.
                dialog1.setPositiveButton(R.string.accept, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        //Vuelve a poner el ProgressBar mientras se baja e instala.
                        loadingPanel.show();
                        if(updater.getDeleteBefore() && updater.getCurrentVersionCode() > 0) {
                            loadingPanel.setMessage("Eliminando version " + updater.getCurrentVersionName() + " anterior...");
                            Uri packageUri = Uri.parse("package:" + Autoupdater.PACKAGE_NAME);
                            Intent uninstallIntent =
                                    new Intent(Intent.ACTION_UNINSTALL_PACKAGE, packageUri);
                            uninstallIntent.putExtra(Intent.EXTRA_RETURN_RESULT, true);
                            activity.startActivityForResult(uninstallIntent, UNINSTALL_CODE);
                            Log.w("Unistall", "Desinstalando apk anterior");
                        }else {
                            loadingPanel.setMessage("Descargando Version " + updater.getLatestVersionName() + "...");
                            updater.InstallNewVersion(ocultarLoadingPanel);
                        }
                    }
                });

                //Muestra la ventana esperando respuesta.
                dialog1.show();
            }else{
                //No existen Actualizaciones.
                Log.d("Mensaje", "No hay actualizaciones");
                textViewActualizaciones.setText("No Hay actualizaciones disponibles.");
                textViewActualizaciones.setTextColor(Color.BLUE);

            }
        }
    };

    private Runnable ocultarLoadingPanel = new Runnable() {
        @Override
        public void run() {
            Intent intent = new Intent(Intent.ACTION_INSTALL_PACKAGE);
            intent.setDataAndType(Uri.fromFile(new File(Environment.getExternalStorageDirectory() + "/download/" + Autoupdater.NAME_APK)), "application/vnd.android.package-archive");
            //intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            intent.putExtra(Intent.EXTRA_RETURN_RESULT, true);
            activity.startActivityForResult(intent, INSTALL_CODE);
            Log.w("Istall", "Instalando nuevo apk");
        }
    };
}
