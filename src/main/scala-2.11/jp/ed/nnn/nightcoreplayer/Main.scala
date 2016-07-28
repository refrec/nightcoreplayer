package jp.ed.nnn.nightcoreplayer

import java.io.File
import javafx.application.Application
import javafx.beans.value.{ChangeListener, ObservableValue}
import javafx.collections.FXCollections
import javafx.event.EventHandler
import javafx.geometry.Pos
import javafx.scene.Scene
import javafx.scene.control.cell.PropertyValueFactory
import javafx.scene.control.{Label, TableColumn, TableView}
import javafx.scene.input.{DragEvent, TransferMode}
import javafx.scene.layout.{BorderPane, HBox}
import javafx.scene.media.{Media, MediaPlayer, MediaView}
import javafx.scene.paint.Color
import javafx.stage.Stage
import javafx.util.Duration

import jp.ed.nnn.movieplayer.Movie

object Main extends App {
  Application.launch(classOf[Main], args: _*)
}

class Main extends Application {

  private[this] val mediaViewFitWidth = 800
  private[this] val mediaViewFitHeight = 450
  private[this] val toolBarMinHeight = 50
  private[this] val tableMinWidth = 300

  override def start(primaryStage: Stage): Unit = {
    val path = "/Users/soichiro_yoshimura/vagrant/ubuntu64/workspace/youtube_download/video.mp4"
    val media = new Media(new File(path).toURI.toString)
    val mediaPlayer = new MediaPlayer(media)
    mediaPlayer.setRate(1.25)
    mediaPlayer.play()
    val mediaView = new MediaView(mediaPlayer)

    val timeLabel = new Label()
    mediaPlayer.currentTimeProperty().addListener(new ChangeListener[Duration] {
      override def changed(observable: ObservableValue[_ <: Duration], oldValue: Duration, newValue: Duration): Unit =
        timeLabel.setText(formatTime(mediaPlayer.getCurrentTime, mediaPlayer.getTotalDuration))
    })
    mediaPlayer.setOnReady(new Runnable {
      override def run(): Unit =
        timeLabel.setText(formatTime(mediaPlayer.getCurrentTime, mediaPlayer.getTotalDuration))
    })
    timeLabel.setText("00:00:00/00:00:00")
    timeLabel.setTextFill(Color.WHITE)

    val toolBar = new HBox(timeLabel)
    toolBar.setMinHeight(toolBarMinHeight)
    toolBar.setAlignment(Pos.CENTER)
    toolBar.setStyle("-fx-background-color: Black")

    val tableView = new TableView[Movie]()
    tableView.setMinWidth(tableMinWidth)
    val movies = FXCollections.observableArrayList[Movie]()
    tableView.setItems(movies)

    val fileNameColumn = new TableColumn[Movie, String]("ファイル名")
    fileNameColumn.setCellValueFactory(new PropertyValueFactory("fileName"))
    fileNameColumn.setPrefWidth(160)
    val timeColumn = new TableColumn[Movie, String]("時間")
    timeColumn.setCellValueFactory(new PropertyValueFactory("time"))
    timeColumn.setPrefWidth(80)

    tableView.getColumns.setAll(fileNameColumn, timeColumn)

    val baseBorderPane = new BorderPane()
    baseBorderPane.setStyle("-fx-background-color: Black")
    baseBorderPane.setCenter(mediaView)
    baseBorderPane.setBottom(toolBar)
    baseBorderPane.setRight(tableView)

    val scene = new Scene(baseBorderPane, mediaViewFitWidth + tableMinWidth, mediaViewFitHeight + toolBarMinHeight)
    scene.setFill(Color.BLACK)
    mediaView.fitWidthProperty().bind(scene.widthProperty().subtract(tableMinWidth))
    mediaView.fitHeightProperty().bind(scene.heightProperty().subtract(toolBarMinHeight))

    scene.setOnDragOver(new EventHandler[DragEvent] {
      override def handle(event: DragEvent): Unit = {
        if (event.getGestureSource != scene &&
          event.getDragboard.hasFiles) {
          event.acceptTransferModes(TransferMode.COPY_OR_MOVE: _*)
        }
        event.consume()
      }
    })

    scene.setOnDragDropped(new EventHandler[DragEvent] {
      override def handle(event: DragEvent): Unit = {
        val db = event.getDragboard
        if (db.hasFiles) {
          db.getFiles.toArray(Array[File]()).toSeq.foreach { f =>
            val filePath = f.getAbsolutePath
            val fileName = f.getName
            val media = new Media(f.toURI.toString)
            val time = formatTime(media.getDuration)
            val movie = Movie(System.currentTimeMillis(), fileName, time, filePath, media)
            while (movies.contains(movie)) {
              movie.setId(movie.getId + 1L)
            }
            movies.add(movie)
          }
      }
        event.consume()
      }
    })

    primaryStage.setTitle("mp4ファイルをドラッグ&ドロップしてください")

    primaryStage.setScene(scene)
    primaryStage.show()
  }

  private[this] def formatTime(elapsed: Duration): String = {
    "%02d:%02d:%02d".format(
      elapsed.toHours.toInt,
      elapsed.toMinutes.toInt % 60,
      elapsed.toSeconds.toInt % 60
    )
  }

  private[this] def formatTime(elapsed: Duration, duration: Duration): String =
    s"${formatTime(elapsed)}/${formatTime(duration)}"
}