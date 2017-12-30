/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Formulario;

import com.digitalpersona.onetouch.DPFPDataPurpose;
import com.digitalpersona.onetouch.DPFPFeatureSet;
import com.digitalpersona.onetouch.DPFPGlobal;
import com.digitalpersona.onetouch.DPFPSample;
import com.digitalpersona.onetouch.DPFPTemplate;
import com.digitalpersona.onetouch.capture.DPFPCapture;
import com.digitalpersona.onetouch.capture.event.DPFPDataAdapter;
import com.digitalpersona.onetouch.capture.event.DPFPDataEvent;
import com.digitalpersona.onetouch.capture.event.DPFPErrorAdapter;
import com.digitalpersona.onetouch.capture.event.DPFPErrorEvent;
import com.digitalpersona.onetouch.capture.event.DPFPReaderStatusAdapter;
import com.digitalpersona.onetouch.capture.event.DPFPReaderStatusEvent;
import com.digitalpersona.onetouch.capture.event.DPFPSensorAdapter;
import com.digitalpersona.onetouch.capture.event.DPFPSensorEvent;
import com.digitalpersona.onetouch.processing.DPFPEnrollment;
import com.digitalpersona.onetouch.processing.DPFPFeatureExtraction;
import com.digitalpersona.onetouch.processing.DPFPImageQualityException;
import com.digitalpersona.onetouch.verification.DPFPVerification;
import java.awt.Image;
import javax.swing.ImageIcon;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import BD.ConexionBD;
import com.digitalpersona.onetouch.verification.DPFPVerificationResult;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;
/**
 *
 * @author alejo
 */

public class CapturaHuella extends javax.swing.JFrame {

    /**
     * Creates new form CapturaHuella
     */
    
    //Variables de Digital Persona
    private DPFPCapture lector = DPFPGlobal.getCaptureFactory().createCapture();
    private DPFPEnrollment reclutador = DPFPGlobal.getEnrollmentFactory().createEnrollment();
    private DPFPVerification verificador = DPFPGlobal.getVerificationFactory().createVerification();
    private DPFPTemplate template;
    public static String TEMPLATE_PROPERTY = "template";
    
    public DPFPFeatureSet featuresIncripcion;
    public DPFPFeatureSet featuresVerificacion;
    
    ConexionBD cn=new ConexionBD();
    
    public DPFPFeatureSet extraerCaracteristicas(DPFPSample sample, DPFPDataPurpose purpose){
        DPFPFeatureExtraction extractor = DPFPGlobal.getFeatureExtractionFactory().createFeatureExtraction();
        try{
        return extractor.createFeatureSet(sample, purpose);
        }catch (DPFPImageQualityException e){
            return null;
        }
    }
    
    public Image CrearImagenHuella(DPFPSample sample){
        return DPFPGlobal.getSampleConversionFactory().createImage(sample);
    }
    
    public void DibujarHuella(Image image){
        lblImagenHuella.setIcon(new ImageIcon(
                image.getScaledInstance(lblImagenHuella.getWidth(),
                        lblImagenHuella.getHeight(),
                        image.SCALE_DEFAULT)
        ));
        repaint();
    }
    
    public void EstadoHuellas(){
        EnviarTexto("Muestra de huellas necesarias para guardar template " 
                + reclutador.getFeaturesNeeded());
    }
  
    public void EnviarTexto(String string){
        textArea.append(string + "\n");
    }
    
    public void start(){
        lector.startCapture();
        EnviarTexto("Utilizando el lector de huella dactilar");
    }
    
    public void stop(){
        lector.stopCapture();
        EnviarTexto("No se está utilizando el lector de huella dactilar");
    }
    
    public DPFPTemplate getTemplate(){
        return template;
    }
    
    public void setTemplate(DPFPTemplate template){
        DPFPTemplate old =this.template;
        this.template = template;
        firePropertyChange(TEMPLATE_PROPERTY, old, template);
    }
    
    //Metodo para procesar la captura
    public void ProcesarCaptura(DPFPSample sample){
        // Procesar la muestra de la huella y crear un conjunto de caracterisitcas con el proposito de inscripcion.
        featuresIncripcion = extraerCaracteristicas(sample, DPFPDataPurpose.DATA_PURPOSE_ENROLLMENT);
        
        // Procesar la muestra de la huella y crear un conjunto de caracteristicas con el proposito de verificacion.
        featuresVerificacion = extraerCaracteristicas(sample, DPFPDataPurpose.DATA_PURPOSE_VERIFICATION);
        
        //Comprobar la calidad de la muestra de la huella y lo añade a su reclutador si es bueno
        if (featuresIncripcion != null)
            try{
                System.out.println("Las Caracteristicas de la huella han sido creadas");
                reclutador.addFeatures(featuresIncripcion);//Agregar las caracteristicas de la huella a la plantilla a crear
                
                // Dibuja la huella dactilar capturada
                Image image=CrearImagenHuella(sample);
                DibujarHuella(image);
                
                btnVerificar.setEnabled(true);
                btnIdentificar.setEnabled(true);
                
            } catch (DPFPImageQualityException ex){
                System.out.println("Error: "+ex.getMessage());
            }
            finally{
                EstadoHuellas();
                // Comprueba si la plantilla se ha creado
                switch(reclutador.getTemplateStatus())
                {
                    case TEMPLATE_STATUS_READY: // informe de éxito y detiene la captura de huella
                        stop();
                        setTemplate(reclutador.getTemplate());
                        EnviarTexto("La plantilla de la huella ha sido creada, ya puede verificarla o identificarla");
                        btnIdentificar.setEnabled(false);
                        btnVerificar.setEnabled(false);
                        btnGuardar.setEnabled(true);
                        btnGuardar.grabFocus();
                        break;
                        
                    case TEMPLATE_STATUS_FAILED: // informe de fallas y reiniciar la captura de huellas
                        reclutador.clear();
                        stop();
                        EstadoHuellas();
                        setTemplate(null);
                        JOptionPane.showMessageDialog(CapturaHuella.this, "La plantilla de la huella no pudo ser creada, por favor repita el proceso");
                        start();
                        break;
                
                }      
            
                
            }
        
            
    }
    
    protected void Iniciar(){
    lector.addDataListener(new DPFPDataAdapter(){
        @Override
        public void dataAcquired(final DPFPDataEvent e){
            SwingUtilities.invokeLater(new Runnable(){
                @Override
                public void run(){
                    EnviarTexto("La Huella Digital ha sido Capturada");
                    ProcesarCaptura(e.getSample());
                }
            });
        }
    });
    
    lector.addReaderStatusListener(new DPFPReaderStatusAdapter(){
        @Override
        public void readerConnected(final DPFPReaderStatusEvent e){
            SwingUtilities.invokeLater(new Runnable(){
                @Override
                public void run(){
                    EnviarTexto("El sensor de huella digital está Activado o Conectado");
                }
            });
        }
        @Override
        public void readerDisconnected(final DPFPReaderStatusEvent e){
            SwingUtilities.invokeLater(new Runnable(){
            @Override
            public void run(){
                EnviarTexto("El sensor de huella digital está Desactivado o No Conectado");
            }
            });
        }
    });
    
    lector.addSensorListener(new DPFPSensorAdapter(){
        @Override
        public void fingerTouched(final DPFPSensorEvent e){
            SwingUtilities.invokeLater(new Runnable(){
                @Override
                public void run(){
                    EnviarTexto("El dedo ha sido colocado sobre el lector de huella");
                }
            });
        }
        @Override
        public void fingerGone(final DPFPSensorEvent e){
         SwingUtilities.invokeLater(new Runnable(){
                @Override
                public void run(){
                    EnviarTexto("El dedo ha sido quitado del lector de huella");
                }
            });
        }
    });
    
    lector.addErrorListener(new DPFPErrorAdapter(){
        public void errorReader(final DPFPErrorEvent e){
         SwingUtilities.invokeLater(new Runnable(){
                @Override
                public void run(){
                    EnviarTexto("Error: "+e.getError());
                }
            });
        }
    });
    }
        
    public CapturaHuella() {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch(Exception e){
            JOptionPane.showMessageDialog(null,"Imposible modificar el tema visual.","LookandFeel Invalidos.",JOptionPane.ERROR_MESSAGE);
        }
            
        initComponents();
    }
    
    public void guardarHuella() throws SQLException{
        //Obtiene los datos del template de la huella actual
        ByteArrayInputStream datosHuella =new ByteArrayInputStream(template.serialize());
        Integer tamaHuella =template.serialize().length;
    
        //Pregunta el nombre la persona a la cual corresponde dicha huella
        String nombre = JOptionPane.showInputDialog("Nombre:");
        try{
            //Establece los valores para la sentencia SQL
            Connection c=cn.connectar();
            System.out.println(c.getSchema());
            PreparedStatement guardarStmt = c.prepareStatement(
            "INSERT INTO somhue(huenombre, huehuella) values(?,?)");
            guardarStmt.setString(1, nombre);
            guardarStmt.setBinaryStream(2, datosHuella,tamaHuella);
            
            //Ejecuta la sentencia
            guardarStmt.execute();
            guardarStmt.close();
            JOptionPane.showMessageDialog(null, "Huella guardada correctamente");
            cn.deconectar();
            btnGuardar.setEnabled(false);
            btnVerificar.grabFocus();
            
        }catch (SQLException ex){
            //Si ocurre un error lo indida en consola
            System.out.println("Error al guardar los datos de la huella.");
            System.out.println(ex);
            JOptionPane.showMessageDialog(null, "Error al guardar los datos de la huella.");
        }finally{
            cn.deconectar();
        }
    }
    
    public void verificarHuella(String nom){
        //Verificar la huella digital actual con otra en la base de datos
        try{
            //Estable los valores para la sentencia SQL
            Connection c=cn.connectar();
            //Obtiene la plantilla correspondiente a la persina indicada
            PreparedStatement verificarStmt = c.prepareStatement(
                    "SELECT huehuella FROM somhue WHERE huenombre = ?");
            verificarStmt.setString(1, nom);
            ResultSet rs = verificarStmt.executeQuery();
            
            //Si se encuentra el nombre en la base de datos
            if (rs.next()){
                // Lee la plantilla de la base de datos
                byte templateBuffer[] = rs.getBytes("huehuella");
                // Crea una nueva plantilla a paritr de la guardada en la base de datos
                DPFPTemplate referenceTemplate =DPFPGlobal.getTemplateFactory().createTemplate(templateBuffer);
                // Envia la plantilla creada al objeto contenedor de Template del componente de huella digital
                setTemplate(referenceTemplate);
                
                //Compara las caracteristicas de la huella recientemente capturada con la
                //plantilla guardada al usuario especifico en la base de datos
                DPFPVerificationResult result = verificador.verify(featuresVerificacion, getTemplate());
                
                // compara las platillas (actual contra la de la base de datos)
                if(result.isVerified())
                    JOptionPane.showMessageDialog(null, "Las huellas capturadas coinciden con las de "+nom,"Verificación de huella",JOptionPane.INFORMATION_MESSAGE);
                else
                    JOptionPane.showMessageDialog(null,"No corresponde la huella con "+nom,"Verificación de huella",JOptionPane.ERROR_MESSAGE);
                
                //Si no concuerda con alguna huella correspondiente al nombre lo indica con un mensaje
            } else {
                JOptionPane.showMessageDialog(null,"No existe un registro de huella para "+nom,"Verificacion de huella",JOptionPane.ERROR_MESSAGE);
            }      
        }catch(SQLException ex){
            // Si ocurre un error lo indica en la consola
            System.out.println("Error: "+ex);
            JOptionPane.showMessageDialog(null,"Error al verificar los datos de la huella.");
        }finally{
            cn.deconectar();
        }  
    }
    
    //Identicar a una persona registrada por medio de su huella digital
    public void identificarHuella() throws IOException{
        try{
            //Establece los valres para la sentencia SQL
            Connection c=cn.connectar();
            
            //Obtiene todas las huellas de la base de datos
            PreparedStatement identificarStmt =
                    c.prepareStatement("SELECT huenombre,huehuella FROM somhue");
            ResultSet rs = identificarStmt.executeQuery();
            
            //Si se encuentra el nombre de la base de datos
            while(rs.next()){
                //Lee la plantilla de la base de datos
                byte templateBuffer[] = rs.getBytes("huehuella");
                String nombre=rs.getString("huenombre");
                //Crea una nueva plantilla a apritr de la guardada en la base de datos
                DPFPTemplate referenceTemplate =
                    DPFPGlobal.getTemplateFactory().createTemplate(templateBuffer);
                
                //Envia la plantilla creada al objeto contenedor de Template del
                //componete de huella digital
                setTemplate(referenceTemplate);
                //Compara las caracteristicas de la huella recientemente capturada con
                //alguna plantilla guardada en la base de datos que coincida con ese tipo
                DPFPVerificationResult result = verificador.verify(featuresVerificacion, getTemplate());
                //compara las plantillas actual con las de la base de datos
                //si encuentra correspondencia dibuja el mapa
                //e indica el nombre de la persona que coincidió.
                if(result.isVerified()){
                //crea la imagen de los datos guardados de las huellas guardadas
                //en la base de datos
                JOptionPane.showMessageDialog(null, "La huella capturada es de "+nombre,"Verificacion de huella",JOptionPane.INFORMATION_MESSAGE);
                return;
                }
            }
            //Si no encuentra alguna huella correspondiente al nombre lo indica con un mensaje
            JOptionPane.showMessageDialog(null, "No existe ningún registro que coincida con la huella","Verificacion de huella",JOptionPane.ERROR_MESSAGE);
            setTemplate(null);
        } catch(SQLException e){
            //Si ocurre un error lo indica en la consola
            System.out.println("Error: "+e.getMessage());
            JOptionPane.showMessageDialog(null,"Error al identificar la huella dactilar. "+e.getMessage());
        } finally {
            cn.deconectar();
        }
    
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */ 
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        panHuella = new javax.swing.JPanel();
        lblImagenHuella = new javax.swing.JLabel();
        panBtns = new javax.swing.JPanel();
        btnVerificar = new javax.swing.JButton();
        btnGuardar = new javax.swing.JButton();
        btnIdentificar = new javax.swing.JButton();
        btnSalir = new javax.swing.JButton();
        jPanel1 = new javax.swing.JPanel();
        jScrollPane1 = new javax.swing.JScrollPane();
        textArea = new javax.swing.JTextArea();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowClosing(java.awt.event.WindowEvent evt) {
                formWindowClosing(evt);
            }
            public void windowOpened(java.awt.event.WindowEvent evt) {
                formWindowOpened(evt);
            }
        });

        panHuella.setBorder(javax.swing.BorderFactory.createTitledBorder(new javax.swing.border.SoftBevelBorder(javax.swing.border.BevelBorder.RAISED), "Huella Digital", javax.swing.border.TitledBorder.CENTER, javax.swing.border.TitledBorder.DEFAULT_POSITION));

        lblImagenHuella.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(0, 0, 0)));

        javax.swing.GroupLayout panHuellaLayout = new javax.swing.GroupLayout(panHuella);
        panHuella.setLayout(panHuellaLayout);
        panHuellaLayout.setHorizontalGroup(
            panHuellaLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(panHuellaLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(lblImagenHuella, javax.swing.GroupLayout.DEFAULT_SIZE, 101, Short.MAX_VALUE)
                .addContainerGap())
        );
        panHuellaLayout.setVerticalGroup(
            panHuellaLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(panHuellaLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(lblImagenHuella, javax.swing.GroupLayout.DEFAULT_SIZE, 108, Short.MAX_VALUE)
                .addContainerGap())
        );

        panBtns.setBorder(javax.swing.BorderFactory.createTitledBorder(new javax.swing.border.SoftBevelBorder(javax.swing.border.BevelBorder.RAISED), "Acciones", javax.swing.border.TitledBorder.CENTER, javax.swing.border.TitledBorder.DEFAULT_POSITION));

        btnVerificar.setText("Verificar");
        btnVerificar.setBorder(new javax.swing.border.SoftBevelBorder(javax.swing.border.BevelBorder.RAISED));
        btnVerificar.setContentAreaFilled(false);
        btnVerificar.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnVerificarActionPerformed(evt);
            }
        });

        btnGuardar.setText("Guardar");
        btnGuardar.setBorder(new javax.swing.border.SoftBevelBorder(javax.swing.border.BevelBorder.RAISED));
        btnGuardar.setContentAreaFilled(false);
        btnGuardar.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnGuardarActionPerformed(evt);
            }
        });

        btnIdentificar.setText("Identificar");
        btnIdentificar.setBorder(new javax.swing.border.SoftBevelBorder(javax.swing.border.BevelBorder.RAISED));
        btnIdentificar.setContentAreaFilled(false);
        btnIdentificar.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnIdentificarActionPerformed(evt);
            }
        });

        btnSalir.setText("Salir");
        btnSalir.setBorder(new javax.swing.border.SoftBevelBorder(javax.swing.border.BevelBorder.RAISED));
        btnSalir.setContentAreaFilled(false);
        btnSalir.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnSalirActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout panBtnsLayout = new javax.swing.GroupLayout(panBtns);
        panBtns.setLayout(panBtnsLayout);
        panBtnsLayout.setHorizontalGroup(
            panBtnsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(panBtnsLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(btnVerificar, javax.swing.GroupLayout.PREFERRED_SIZE, 55, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(48, 48, 48)
                .addComponent(btnIdentificar)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 142, Short.MAX_VALUE)
                .addComponent(btnGuardar, javax.swing.GroupLayout.PREFERRED_SIZE, 53, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(45, 45, 45)
                .addComponent(btnSalir, javax.swing.GroupLayout.PREFERRED_SIZE, 53, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(24, 24, 24))
        );
        panBtnsLayout.setVerticalGroup(
            panBtnsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(panBtnsLayout.createSequentialGroup()
                .addGap(24, 24, 24)
                .addGroup(panBtnsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(btnVerificar, javax.swing.GroupLayout.PREFERRED_SIZE, 30, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(btnIdentificar, javax.swing.GroupLayout.PREFERRED_SIZE, 29, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(btnGuardar, javax.swing.GroupLayout.PREFERRED_SIZE, 30, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(btnSalir, javax.swing.GroupLayout.PREFERRED_SIZE, 27, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(20, Short.MAX_VALUE))
        );

        textArea.setColumns(20);
        textArea.setRows(5);
        jScrollPane1.setViewportView(textArea);

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addComponent(jScrollPane1)
                .addContainerGap())
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel1Layout.createSequentialGroup()
                .addGap(0, 0, Short.MAX_VALUE)
                .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 136, javax.swing.GroupLayout.PREFERRED_SIZE))
        );

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(panBtns, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap())
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(panHuella, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(187, 187, 187))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGap(20, 20, 20)
                .addComponent(panHuella, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(18, 18, 18)
                .addComponent(panBtns, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(18, 18, 18)
                .addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addContainerGap())
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void btnSalirActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnSalirActionPerformed
       System.exit(0);
    }//GEN-LAST:event_btnSalirActionPerformed

    private void btnVerificarActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnVerificarActionPerformed
        String nombre = JOptionPane.showInputDialog("Nombre a verificar: ");
        verificarHuella(nombre);
        reclutador.clear();
    }//GEN-LAST:event_btnVerificarActionPerformed

    private void btnGuardarActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnGuardarActionPerformed
        try{
            guardarHuella();
            reclutador.clear();
            lblImagenHuella.setIcon(null);
            start();
        }catch(SQLException ex){
            Logger.getLogger(CapturaHuella.class.getName()).log(Level.SEVERE,null,ex);
        }
    }//GEN-LAST:event_btnGuardarActionPerformed

    private void btnIdentificarActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnIdentificarActionPerformed
        try{
            identificarHuella();
            reclutador.clear();
        }catch(IOException ex){
            Logger.getLogger(CapturaHuella.class.getName()).log(Level.SEVERE,null,ex);
        }
    }//GEN-LAST:event_btnIdentificarActionPerformed

    private void formWindowClosing(java.awt.event.WindowEvent evt) {//GEN-FIRST:event_formWindowClosing
        stop();
    }//GEN-LAST:event_formWindowClosing

    private void formWindowOpened(java.awt.event.WindowEvent evt) {//GEN-FIRST:event_formWindowOpened
        Iniciar();
        start();
        EstadoHuellas();
        btnGuardar.setEnabled(false);
        btnIdentificar.setEnabled(false);
        btnVerificar.setEnabled(false);
        btnSalir.grabFocus();
    }//GEN-LAST:event_formWindowOpened

    /**
     * @param args the command line arguments
     */
    public static void main(String args[]) {
        /* Set the Nimbus look and feel */
        //<editor-fold defaultstate="collapsed" desc=" Look and feel setting code (optional) ">
        /* If Nimbus (introduced in Java SE 6) is not available, stay with the default look and feel.
         * For details see http://download.oracle.com/javase/tutorial/uiswing/lookandfeel/plaf.html 
         */
        try {
            for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    javax.swing.UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (ClassNotFoundException ex) {
            java.util.logging.Logger.getLogger(CapturaHuella.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (InstantiationException ex) {
            java.util.logging.Logger.getLogger(CapturaHuella.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            java.util.logging.Logger.getLogger(CapturaHuella.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (javax.swing.UnsupportedLookAndFeelException ex) {
            java.util.logging.Logger.getLogger(CapturaHuella.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
        //</editor-fold>

        /* Create and display the form */
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                new CapturaHuella().setVisible(true);
            }
        });
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton btnGuardar;
    private javax.swing.JButton btnIdentificar;
    private javax.swing.JButton btnSalir;
    private javax.swing.JButton btnVerificar;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JLabel lblImagenHuella;
    private javax.swing.JPanel panBtns;
    private javax.swing.JPanel panHuella;
    private javax.swing.JTextArea textArea;
    // End of variables declaration//GEN-END:variables
}
