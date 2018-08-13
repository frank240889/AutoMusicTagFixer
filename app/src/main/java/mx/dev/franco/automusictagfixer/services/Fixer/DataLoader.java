package mx.dev.franco.automusictagfixer.services.Fixer;

public interface DataLoader<DATA>{
    void onDataLoaded(DATA data);
}