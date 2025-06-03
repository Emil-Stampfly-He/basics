use tokio::sync::broadcast;

async fn broadcast() {
    let (tx, _rx) = broadcast::channel::<String>(16);
    
    let mut rx_a = tx.subscribe();
    tokio::spawn(async move {
        let string = rx_a.recv().await.unwrap();
        println!("{}", string);
    });
    
    let mut rx_b = tx.subscribe();
    tokio::spawn(async move {
        let string = rx_b.recv().await.unwrap();
        println!("{}", string);
    });
}