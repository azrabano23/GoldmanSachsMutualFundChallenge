import { Component, ElementRef, ViewChild, ChangeDetectorRef } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { FormsModule } from '@angular/forms';
import { DecimalPipe, PercentPipe, NgClass } from '@angular/common';
import { Chart } from 'chart.js/auto';
import { forkJoin, of } from 'rxjs';

const API = 'http://localhost:8080';

interface Fund { name: string; ticker: string; }

interface MonteCarloResult {
  ticker: string; principal: number; years: number; simulations: number;
  p10FinalValue: number; p50FinalValue: number; p90FinalValue: number;
  meanFinalValue: number; stdDevFinalValue: number;
  historicalMeanAnnualReturn: number; historicalStdDevAnnual: number;
  p10Path: number[]; p50Path: number[]; p90Path: number[];
}

interface SharpeResult {
  ticker: string; yearsOfData: number;
  annualReturn: number; annualStdDev: number;
  riskFreeRate: number; sharpeRatio: number; interpretation: string;
}

interface FundAnalysis {
  ticker: string; fundName: string;
  beta: number; oneYearReturn: number;
  strategyOverview: string; riskAssessment: string;
  investorProfile: string; keyConsiderations: string[]; summary: string;
}

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [FormsModule, DecimalPipe, PercentPipe, NgClass],
  templateUrl: './app.html',
  styleUrls: ['./app.css']
})
export class App {
  // ─── Tab navigation ───────────────────────────────────────────────────────
  activeTab: 'capm' | 'montecarlo' | 'compare' | 'sharpe' | 'ai' = 'capm';

  // ─── Shared fund list (loaded from backend) ────────────────────────────────
  funds: Fund[] = [];

  // ─── CAPM Projection ──────────────────────────────────────────────────────
  @ViewChild('capmCanvas') capmCanvas?: ElementRef<HTMLCanvasElement>;
  capmFund = 'VSMPX';
  capmAmount = 10000;
  capmYears = 10;
  capmResult: number | null = null;
  capmError: string | null = null;
  capmLoading = false;
  private capmChart: Chart | null = null;

  // ─── Monte Carlo ──────────────────────────────────────────────────────────
  @ViewChild('mcCanvas') mcCanvas?: ElementRef<HTMLCanvasElement>;
  mcFund = 'VSMPX';
  mcAmount = 10000;
  mcYears = 10;
  mcSims = 1000;
  mcResult: MonteCarloResult | null = null;
  mcError: string | null = null;
  mcLoading = false;
  private mcChart: Chart | null = null;

  // ─── Multi-Fund Comparison ────────────────────────────────────────────────
  @ViewChild('compareCanvas') compareCanvas?: ElementRef<HTMLCanvasElement>;
  compareAmount = 10000;
  compareYears = 10;
  compareFunds: string[] = ['VSMPX', 'FXAIX', 'VFIAX'];
  compareResult: Record<string, number[]> | null = null;
  compareError: string | null = null;
  compareLoading = false;
  private compareChart: Chart | null = null;

  // ─── Sharpe Ratio ─────────────────────────────────────────────────────────
  sharpeFund = 'VSMPX';
  sharpeYears = 3;
  sharpeResult: SharpeResult | null = null;
  sharpeError: string | null = null;
  sharpeLoading = false;

  // ─── AI Fund Analyst ──────────────────────────────────────────────────────
  aiFund = 'VSMPX';
  aiResult: FundAnalysis | null = null;
  aiError: string | null = null;
  aiLoading = false;

  // ─── Chart palette ────────────────────────────────────────────────────────
  private readonly COLORS = [
    '#1f6feb', '#3fb950', '#f78166', '#d2a8ff', '#ffa657',
    '#79c0ff', '#56d364', '#ff7b72', '#e3b341', '#8b949e'
  ];

  constructor(private http: HttpClient, private cdr: ChangeDetectorRef) {
    this.http.get<Fund[]>(`${API}/funds`).subscribe({
      next: (funds) => { this.funds = funds; },
      error: () => {
        // fallback if backend not running
        this.funds = [
          { ticker: 'VSMPX', name: 'Vanguard Total Stock Market Index Fund Inst+' },
          { ticker: 'FXAIX', name: 'Fidelity 500 Index Fund' },
          { ticker: 'VFIAX', name: 'Vanguard 500 Index Fund' },
          { ticker: 'VTSAX', name: 'Vanguard Total Stock Market Index Admiral' },
          { ticker: 'VGTSX', name: 'Vanguard Total International Stock Index' },
          { ticker: 'FCNTX', name: 'Fidelity Contrafund' },
          { ticker: 'AGTHX', name: 'American Funds Growth Fund of America' },
          { ticker: 'DODGX', name: 'Dodge & Cox Stock Fund' },
          { ticker: 'VWELX', name: 'Vanguard Wellington Fund' },
          { ticker: 'PRDGX', name: 'T. Rowe Price Dividend Growth Fund' },
        ];
      }
    });
  }

  // ─── CAPM Projection ──────────────────────────────────────────────────────

  calculateCapm() {
    this.capmError = null;
    this.capmLoading = true;
    this.capmResult = null;
    this.capmChart?.destroy();
    this.capmChart = null;

    if (this.capmAmount <= 0 || this.capmYears <= 0) {
      this.capmError = 'Please enter amount and years greater than 0.';
      this.capmLoading = false;
      return;
    }

    const timePoints = this.buildYearPoints(this.capmYears);
    const requests = timePoints.map(t =>
      t === 0 ? of(this.capmAmount) :
        this.http.get<number>(`${API}/funds/future-value/${this.capmFund}`,
          { params: new HttpParams().set('principal', this.capmAmount).set('years', t) })
    );

    forkJoin(requests).subscribe({
      next: (values) => {
        this.capmResult = values[values.length - 1] ?? null;
        setTimeout(() => this.renderCapmChart(timePoints, values as number[]), 0);
        this.capmLoading = false;
        this.cdr.detectChanges();
      },
      error: () => {
        this.capmError = 'Could not reach backend. Is the Spring server running on port 8080?';
        this.capmLoading = false;
        this.cdr.detectChanges();
      }
    });
  }

  private renderCapmChart(labels: number[], data: number[]) {
    const ctx = this.capmCanvas?.nativeElement?.getContext('2d');
    if (!ctx) return;
    this.capmChart?.destroy();
    this.capmChart = new Chart(ctx, {
      type: 'line',
      data: {
        labels: labels.map(v => `Year ${v}`),
        datasets: [{
          label: `${this.capmFund} — CAPM projection`,
          data: data.map(v => +v.toFixed(2)),
          borderColor: '#1f6feb',
          backgroundColor: 'rgba(31,111,235,0.1)',
          fill: true, tension: 0.25, pointRadius: 3
        }]
      },
      options: { responsive: true, maintainAspectRatio: false }
    });
  }

  // ─── Monte Carlo ──────────────────────────────────────────────────────────

  runMonteCarlo() {
    this.mcError = null;
    this.mcLoading = true;
    this.mcResult = null;
    this.mcChart?.destroy();
    this.mcChart = null;

    if (this.mcAmount <= 0 || this.mcYears <= 0) {
      this.mcError = 'Please enter amount and years greater than 0.';
      this.mcLoading = false;
      return;
    }

    const params = new HttpParams()
      .set('ticker', this.mcFund)
      .set('principal', this.mcAmount)
      .set('years', this.mcYears)
      .set('simulations', this.mcSims);

    this.http.get<MonteCarloResult>(`${API}/funds/monte-carlo`, { params }).subscribe({
      next: (result) => {
        this.mcResult = result;
        setTimeout(() => this.renderMcChart(result), 0);
        this.mcLoading = false;
        this.cdr.detectChanges();
      },
      error: () => {
        this.mcError = 'Monte Carlo simulation failed. Check backend connection.';
        this.mcLoading = false;
        this.cdr.detectChanges();
      }
    });
  }

  private renderMcChart(r: MonteCarloResult) {
    const ctx = this.mcCanvas?.nativeElement?.getContext('2d');
    if (!ctx) return;
    this.mcChart?.destroy();
    const months = r.p50Path.length;
    const labels = Array.from({ length: months }, (_, i) => {
      const yr = Math.floor(i / 12);
      const mo = i % 12;
      return mo === 0 ? `Yr ${yr}` : '';
    });

    this.mcChart = new Chart(ctx, {
      type: 'line',
      data: {
        labels,
        datasets: [
          {
            label: 'Optimistic (p90)',
            data: r.p90Path.map(v => +v.toFixed(2)),
            borderColor: '#3fb950', backgroundColor: 'rgba(63,185,80,0.15)',
            fill: '+1', tension: 0.3, pointRadius: 0, borderWidth: 2
          },
          {
            label: 'Median (p50)',
            data: r.p50Path.map(v => +v.toFixed(2)),
            borderColor: '#1f6feb', backgroundColor: 'rgba(31,111,235,0.1)',
            fill: false, tension: 0.3, pointRadius: 0, borderWidth: 2.5
          },
          {
            label: 'Pessimistic (p10)',
            data: r.p10Path.map(v => +v.toFixed(2)),
            borderColor: '#f78166', backgroundColor: 'rgba(247,129,102,0.1)',
            fill: false, tension: 0.3, pointRadius: 0, borderWidth: 2
          }
        ]
      },
      options: {
        responsive: true, maintainAspectRatio: false,
        plugins: { tooltip: { mode: 'index', intersect: false } },
        scales: { x: { ticks: { maxRotation: 0 } } }
      }
    });
  }

  // ─── Multi-Fund Comparison ────────────────────────────────────────────────

  addCompareFund() {
    if (this.compareFunds.length < 5) this.compareFunds.push('VSMPX');
  }

  removeCompareFund(i: number) {
    if (this.compareFunds.length > 2) this.compareFunds.splice(i, 1);
  }

  runComparison() {
    this.compareError = null;
    this.compareLoading = true;
    this.compareResult = null;
    this.compareChart?.destroy();
    this.compareChart = null;

    if (this.compareAmount <= 0 || this.compareYears <= 0) {
      this.compareError = 'Please enter amount and years greater than 0.';
      this.compareLoading = false;
      return;
    }

    const tickers = [...new Set(this.compareFunds)].join(',');
    const params = new HttpParams()
      .set('tickers', tickers)
      .set('principal', this.compareAmount)
      .set('years', this.compareYears);

    this.http.get<Record<string, number[]>>(`${API}/funds/compare`, { params }).subscribe({
      next: (result) => {
        this.compareResult = result;
        setTimeout(() => this.renderCompareChart(result), 0);
        this.compareLoading = false;
        this.cdr.detectChanges();
      },
      error: () => {
        this.compareError = 'Comparison failed. Check backend connection.';
        this.compareLoading = false;
        this.cdr.detectChanges();
      }
    });
  }

  private renderCompareChart(data: Record<string, number[]>) {
    const ctx = this.compareCanvas?.nativeElement?.getContext('2d');
    if (!ctx) return;
    this.compareChart?.destroy();

    const tickers = Object.keys(data);
    const months = data[tickers[0]].length;
    const labels = Array.from({ length: months }, (_, i) => {
      const yr = Math.floor(i / 12);
      const mo = i % 12;
      return mo === 0 ? `Yr ${yr}` : '';
    });

    const datasets = tickers.map((ticker, idx) => ({
      label: ticker,
      data: data[ticker].map(v => +v.toFixed(2)),
      borderColor: this.COLORS[idx % this.COLORS.length],
      backgroundColor: 'transparent',
      fill: false, tension: 0.3, pointRadius: 0, borderWidth: 2.5
    }));

    this.compareChart = new Chart(ctx, {
      type: 'line',
      data: { labels, datasets },
      options: {
        responsive: true, maintainAspectRatio: false,
        plugins: { tooltip: { mode: 'index', intersect: false } },
        scales: { x: { ticks: { maxRotation: 0 } } }
      }
    });
  }

  // ─── Sharpe Ratio ─────────────────────────────────────────────────────────

  runSharpe() {
    this.sharpeError = null;
    this.sharpeLoading = true;
    this.sharpeResult = null;

    const params = new HttpParams()
      .set('ticker', this.sharpeFund)
      .set('years', this.sharpeYears);

    this.http.get<SharpeResult>(`${API}/funds/sharpe`, { params }).subscribe({
      next: (result) => {
        this.sharpeResult = result;
        this.sharpeLoading = false;
        this.cdr.detectChanges();
      },
      error: () => {
        this.sharpeError = 'Sharpe calculation failed. Check backend connection.';
        this.sharpeLoading = false;
        this.cdr.detectChanges();
      }
    });
  }

  sharpeColor(ratio: number): string {
    if (ratio >= 2) return 'stat-excellent';
    if (ratio >= 1) return 'stat-good';
    if (ratio >= 0) return 'stat-ok';
    return 'stat-poor';
  }

  // ─── AI Fund Analyst ──────────────────────────────────────────────────────

  runAiAnalysis() {
    this.aiError = null;
    this.aiLoading = true;
    this.aiResult = null;

    this.http.post<FundAnalysis>(`${API}/ai/analyze/${this.aiFund}`, {}).subscribe({
      next: (result) => {
        this.aiResult = result;
        this.aiLoading = false;
        this.cdr.detectChanges();
      },
      error: () => {
        this.aiError = 'AI analysis unavailable. Check backend connection and API key.';
        this.aiLoading = false;
        this.cdr.detectChanges();
      }
    });
  }

  // ─── Helpers ──────────────────────────────────────────────────────────────

  private buildYearPoints(years: number): number[] {
    const points: number[] = [0];
    for (let y = 1; y <= Math.floor(years); y++) points.push(y);
    if (!Number.isInteger(years)) points.push(years);
    return points;
  }

  formatPct(v: number): string {
    return (v * 100).toFixed(2) + '%';
  }
}
